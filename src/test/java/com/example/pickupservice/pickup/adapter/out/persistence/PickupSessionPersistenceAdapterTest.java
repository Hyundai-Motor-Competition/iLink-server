package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.ChildSchedule;
import com.example.pickupservice.pickup.domain.IncidentType;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class PickupSessionPersistenceAdapterTest extends IntegrationTestSupport {

	/** 소수점 6자리까지 같다 = 차이가 0.0000005 미만. */
	private static final double SIX_DECIMALS = 0.0000005;
	private static final String UNIQUE_CHILD_PER_SESSION = "uk_session_child_session_child_user";
	private static final String SNAPSHOT_SPOT_NAME = "후문 앞 스냅샷";
	/** 시드 장소 좌표와 다른 값: 장소 테이블이 아니라 세션 스냅샷에서 읽는지 본다. */
	private static final LatLng SNAPSHOT_STOP = new LatLng(37.501777, 127.039123);
	private static final LatLng SNAPSHOT_MEETING = new LatLng(37.501888, 127.039456);
	private static final LatLng CHILD_LOCATION = new LatLng(37.501555, 127.039222);
	private static final LatLng VEHICLE_LOCATION = new LatLng(37.498123, 127.027678);
	private static final LatLng NEWER_VEHICLE_LOCATION = new LatLng(37.499456, 127.028789);
	private static final LatLng SEED_STOP = new LatLng(PickupSeed.STOP_LAT, PickupSeed.STOP_LNG);
	private static final LatLng SEED_MEETING = new LatLng(PickupSeed.MEETING_LAT, PickupSeed.MEETING_LNG);
	private static final Instant T0 = Instant.parse("2026-11-09T07:00:00Z");
	/** 탑승 취소 창 10초: boardingDetectedAt = boardingCancelableUntil - 10초. */
	private static final long BOARDING_WINDOW_SECONDS = 10;
	/** 앞 트랜잭션이 잠근 동안 뒤 트랜잭션이 아직 끝나지 않았는지 보는 시간. */
	private static final long STILL_WAITING_CHECK_MILLIS = 1_000;
	/** 신호 · 결과를 기다리는 최대 시간. 정상이면 훨씬 빨리 끝난다. */
	private static final long MAX_WAIT_SECONDS = 30;
	/** 앞 트랜잭션이 잠근 채로 바꿔 커밋하는 stateVersion. 시드 값 0과 다르다. */
	private static final int STATE_VERSION_COMMITTED_BY_FIRST = 5;

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final PickupSeed pickupSeed;
	private final UserSeed userSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private final List<UserSeed.Result> otherFamilies = new ArrayList<>();
	private PickupSeed.Result seeded;

	@BeforeEach
	void seed() {
		seeded = pickupSeed.seed();
	}

	@AfterEach
	void clear() {
		List<Long> vehicleIds = new ArrayList<>();
		vehicleIds.add(seeded.vehicleId());
		otherFamilies.forEach(family -> vehicleIds.add(family.vehicleId()));
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("""
							delete from SessionChildEntity c
							where c.sessionId in (select s.sessionId from PickupSessionEntity s where s.vehicleId in :vehicleIds)
							""")
					.setParameter("vehicleIds", vehicleIds)
					.executeUpdate();
			entityManager.createQuery("delete from PickupSessionEntity s where s.vehicleId in :vehicleIds")
					.setParameter("vehicleIds", vehicleIds)
					.executeUpdate();
		});
		otherFamilies.forEach(userSeed::clear);
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("PickupSession을 SessionChild 2명과 함께 저장하고 load로 읽으면 spotName · stopPoint · meetingPoint 스냅샷(좌표는 소수점 6자리)과 상태 · 버전 · 시각 · 탑승 · incident 값, 자녀별 childPhase · extraPrepMinutes · callNotBefore · lastLocation · 시각이 같다")
	void savedSessionWithChildrenIsLoadedWithSameSnapshotPointsAndChildStates() {
		SessionChild minjun = SessionChild.restore(null, seeded.minjunId(), seeded.minjunScheduleId(),
				ChildPhase.MEETING, 5, T0.plusSeconds(300), CHILD_LOCATION, T0.plusSeconds(660), T0.plusSeconds(540),
				T0.plusSeconds(900));
		SessionChild seoyun = SessionChild.restore(null, seeded.seoyunId(), seeded.seoyunScheduleId(),
				ChildPhase.CALLED, 0, null, null, null, null, null);
		PickupSession toSave = PickupSession.restore(null, seeded.vehicleId(), seeded.familyId(),
				seeded.otherSpotId(), SNAPSHOT_SPOT_NAME, SNAPSHOT_STOP, SNAPSHOT_MEETING, seeded.date(),
				SessionStatus.ON_HOLD, VehiclePhase.STOPPED, SpotStatus.CONFIRMED, 7, 2, 3, 2, true,
				T0.plusSeconds(720), 350, T0.plusSeconds(600), T0.plusSeconds(900), T0.plusSeconds(910),
				BoardingDoor.REAR_RIGHT, IncidentType.CHILD_NOT_ON_BOARD, "자녀가 차에 타지 않음", null, T0.plusSeconds(960),
				T0.plusSeconds(1200), List.of(minjun, seoyun));

		PickupSession saved = saveSessionPort.save(toSave);

		assertThat(saved.getSessionId()).as("저장하면 sessionId가 생긴다").isNotNull();
		assertThat(saved.getChildren()).extracting(SessionChild::getSessionChildId)
				.as("저장하면 자녀마다 sessionChildId가 생긴다")
				.hasSize(2)
				.doesNotContainNull();

		Map<String, Object> row = jdbcTemplate.queryForMap("""
				select spot_id, spot_name, session_status,
				       st_y(stop_point::geometry) as stop_lat, st_x(stop_point::geometry) as stop_lng,
				       st_y(meeting_point::geometry) as meeting_lat, st_x(meeting_point::geometry) as meeting_lng
				from pickup_session
				where session_id = ?
				""", saved.getSessionId());
		assertThat(((Number) row.get("spot_id")).longValue()).isEqualTo(seeded.otherSpotId());
		assertThat(row.get("spot_name")).isEqualTo(SNAPSHOT_SPOT_NAME);
		assertThat(row.get("session_status")).isEqualTo("ON_HOLD");
		assertThat((Double) row.get("stop_lat")).as("DB stop_point lat")
				.isCloseTo(SNAPSHOT_STOP.lat(), within(SIX_DECIMALS));
		assertThat((Double) row.get("stop_lng")).as("DB stop_point lng")
				.isCloseTo(SNAPSHOT_STOP.lng(), within(SIX_DECIMALS));
		assertThat((Double) row.get("meeting_lat")).as("DB meeting_point lat")
				.isCloseTo(SNAPSHOT_MEETING.lat(), within(SIX_DECIMALS));
		assertThat((Double) row.get("meeting_lng")).as("DB meeting_point lng")
				.isCloseTo(SNAPSHOT_MEETING.lng(), within(SIX_DECIMALS));
		assertThat(jdbcTemplate.queryForList("select child_user_id, child_phase from session_child where session_id = ?",
				saved.getSessionId()))
				.as("세션 자녀 행")
				.extracting(child -> ((Number) child.get("child_user_id")).longValue(), child -> child.get("child_phase"))
				.containsExactlyInAnyOrder(tuple(seeded.minjunId(), "MEETING"), tuple(seeded.seoyunId(), "CALLED"));

		PickupSession loaded = loadSessionPort.load(saved.getSessionId()).orElseThrow();

		assertThat(loaded.getSessionId()).isEqualTo(saved.getSessionId());
		assertThat(loaded).usingRecursiveComparison()
				.ignoringFields("sessionId", "stopPoint", "meetingPoint", "children")
				.isEqualTo(toSave);
		assertLatLng(loaded.getStopPoint(), SNAPSHOT_STOP, "stopPoint");
		assertLatLng(loaded.getMeetingPoint(), SNAPSHOT_MEETING, "meetingPoint");
		assertThat(loaded.getChildren()).extracting(SessionChild::getChildUserId)
				.containsExactlyInAnyOrder(seeded.minjunId(), seeded.seoyunId());

		SessionChild loadedMinjun = childOf(loaded, seeded.minjunId());
		assertThat(loadedMinjun.getSessionChildId()).isEqualTo(childOf(saved, seeded.minjunId()).getSessionChildId());
		assertThat(loadedMinjun).usingRecursiveComparison()
				.ignoringFields("sessionChildId", "lastLocation")
				.isEqualTo(minjun);
		assertLatLng(loadedMinjun.getLastLocation(), CHILD_LOCATION, "민준 lastLocation");

		SessionChild loadedSeoyun = childOf(loaded, seeded.seoyunId());
		assertThat(loadedSeoyun.getSessionChildId()).isEqualTo(childOf(saved, seeded.seoyunId()).getSessionChildId());
		assertThat(loadedSeoyun).usingRecursiveComparison()
				.ignoringFields("sessionChildId")
				.isEqualTo(seoyun);
		assertThat(loadedSeoyun.getLastLocation()).as("서윤 lastLocation").isNull();
	}

	@Test
	@DisplayName("탑승 예외 대상 자녀(incidentChildUserId)는 pickup_session.incident_child_user_id에 저장되고 load로 읽으면 같은 값이다")
	void incidentChildUserIdIsStoredInIncidentChildUserIdColumnAndLoaded() {
		SessionChild minjun = SessionChild.restore(null, seeded.minjunId(), seeded.minjunScheduleId(),
				ChildPhase.MEETING, 0, null, null, null, T0.plusSeconds(300), null);
		PickupSession toSave = PickupSession.restore(null, seeded.vehicleId(), seeded.familyId(),
				seeded.otherSpotId(), SNAPSHOT_SPOT_NAME, SNAPSHOT_STOP, SNAPSHOT_MEETING, seeded.date(),
				SessionStatus.ON_HOLD, VehiclePhase.STOPPED, SpotStatus.CONFIRMED, 7, 0, 1, 0, false,
				T0.plusSeconds(720), 0, T0.plusSeconds(600), T0.plusSeconds(900), T0.plusSeconds(910),
				BoardingDoor.REAR_RIGHT, IncidentType.CHILD_NOT_ON_BOARD, null, seeded.minjunId(), T0.plusSeconds(960),
				null, List.of(minjun));

		PickupSession saved = saveSessionPort.save(toSave);

		assertThat(jdbcTemplate.queryForObject("select incident_child_user_id from pickup_session where session_id = ?",
				Long.class, saved.getSessionId())).isEqualTo(seeded.minjunId());
		PickupSession loaded = loadSessionPort.load(saved.getSessionId()).orElseThrow();
		assertThat(loaded.getIncidentChildUserId()).isEqualTo(seeded.minjunId());
		assertThat(loaded.getIncidentType()).isEqualTo(IncidentType.CHILD_NOT_ON_BOARD);
		assertThat(loaded.getIncidentOpenedAt()).isEqualTo(T0.plusSeconds(960));
	}

	@Test
	@DisplayName("sessionId가 있는 세션을 다시 저장하면 새 세션 · 세션 자녀 행을 만들지 않고 그 행의 stateVersion · callHeld · childPhase · lastLat · lastLng가 바뀐다")
	void resavingExistingSessionUpdatesRowsWithoutInsertingNewRows() {
		PickupSession session = seededSession(SessionStatus.ACTIVE);
		session.markChildrenCalled();
		session.holdCall();
		session.updateChildLocation(seeded.minjunId(), CHILD_LOCATION, T0);

		PickupSession saved = saveSessionPort.save(session);

		assertThat(saved.getSessionId()).as("돌려받은 sessionId").isEqualTo(seeded.sessionId());
		assertThat(jdbcTemplate.queryForObject("select count(*) from pickup_session where vehicle_id = ?",
				Integer.class, seeded.vehicleId())).as("시드 차량 세션 행 수").isEqualTo(1);
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select state_version, call_held from pickup_session where session_id = ?", seeded.sessionId());
		assertThat(((Number) row.get("state_version")).intValue()).isEqualTo(2);
		assertThat(row.get("call_held")).isEqualTo(true);

		List<Map<String, Object>> childRows = jdbcTemplate.queryForList("""
				select session_child_id, child_user_id, child_phase, last_lat, last_lng
				from session_child
				where session_id = ?
				""", seeded.sessionId());
		assertThat(childRows).as("세션 자녀 행")
				.extracting(child -> ((Number) child.get("session_child_id")).longValue(),
						child -> child.get("child_phase"))
				.containsExactlyInAnyOrder(tuple(seeded.minjunSessionChildId(), "CALLED"),
						tuple(seeded.seoyunSessionChildId(), "CALLED"));
		Map<String, Object> minjunRow = childRows.stream()
				.filter(child -> ((Number) child.get("child_user_id")).longValue() == seeded.minjunId())
				.findFirst()
				.orElseThrow();
		assertThat((BigDecimal) minjunRow.get("last_lat")).as("민준 last_lat")
				.isEqualByComparingTo(new BigDecimal("37.501555"));
		assertThat((BigDecimal) minjunRow.get("last_lng")).as("민준 last_lng")
				.isEqualByComparingTo(new BigDecimal("127.039222"));
	}

	@Test
	@DisplayName("마지막 차량 위치가 있는 세션을 저장하면 vehicle_lat · vehicle_lng(소수점 6자리) · vehicle_located_at 컬럼에 담기고 load로 읽으면 vehicleLocation · vehicleLocatedAt이 같다")
	void vehicleLocationIsStoredInVehicleColumnsAndLoaded() {
		PickupSession toSave = PickupSession.restore(seeded.sessionId(), seeded.vehicleId(), seeded.familyId(),
				seeded.spotId(), PickupSeed.SPOT_NAME, SEED_STOP, SEED_MEETING, seeded.date(), SessionStatus.ACTIVE,
				VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, PickupSeed.STATE_VERSION, 0, 1, 0, false,
				T0.plusSeconds(720), 650, T0.plusSeconds(600), null, null, null, null, null, null, null, null,
				VEHICLE_LOCATION, T0.plusSeconds(600), List.of(
						seededChild(seeded.minjunSessionChildId(), seeded.minjunId(), seeded.minjunScheduleId()),
						seededChild(seeded.seoyunSessionChildId(), seeded.seoyunId(), seeded.seoyunScheduleId())));

		saveSessionPort.save(toSave);

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select vehicle_lat, vehicle_lng, vehicle_located_at from pickup_session where session_id = ?",
				seeded.sessionId());
		assertThat((BigDecimal) row.get("vehicle_lat")).as("DB vehicle_lat")
				.isEqualByComparingTo(new BigDecimal("37.498123"));
		assertThat((BigDecimal) row.get("vehicle_lng")).as("DB vehicle_lng")
				.isEqualByComparingTo(new BigDecimal("127.027678"));
		assertThat(row.get("vehicle_located_at")).as("DB vehicle_located_at").isNotNull();
		PickupSession loaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();
		assertLatLng(loaded.getVehicleLocation(), VEHICLE_LOCATION, "vehicleLocation");
		assertThat(loaded.getVehicleLocatedAt()).isEqualTo(T0.plusSeconds(600));
	}

	@Test
	@DisplayName("차량 위치가 없는 세션을 저장하면 vehicle_lat · vehicle_lng · vehicle_located_at이 NULL이고 load로 읽으면 vehicleLocation · vehicleLocatedAt이 null이다")
	void sessionWithoutVehicleLocationStoresNullColumnsAndLoadsNull() {
		saveSessionPort.save(seededSession(SessionStatus.ACTIVE));

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select vehicle_lat, vehicle_lng, vehicle_located_at from pickup_session where session_id = ?",
				seeded.sessionId());
		assertThat(row.get("vehicle_lat")).as("DB vehicle_lat").isNull();
		assertThat(row.get("vehicle_lng")).as("DB vehicle_lng").isNull();
		assertThat(row.get("vehicle_located_at")).as("DB vehicle_located_at").isNull();
		PickupSession loaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();
		assertThat(loaded.getVehicleLocation()).isNull();
		assertThat(loaded.getVehicleLocatedAt()).isNull();
	}

	@Test
	@DisplayName("차량 위치가 저장된 세션에 더 새 위치를 applyTelemetry로 덮어 다시 저장하면 vehicle_lat · vehicle_lng · vehicle_located_at이 새 값으로 바뀐다")
	void resavingSessionOverwritesVehicleLocationColumns() {
		PickupSession session = seededSession(SessionStatus.ACTIVE);
		session.applyTelemetry(T0, 600, 4_200, VEHICLE_LOCATION);
		saveSessionPort.save(session);
		PickupSession reloaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();

		reloaded.applyTelemetry(T0.plusSeconds(15), 500, 3_900, NEWER_VEHICLE_LOCATION);
		saveSessionPort.save(reloaded);

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select vehicle_lat, vehicle_lng from pickup_session where session_id = ?", seeded.sessionId());
		assertThat((BigDecimal) row.get("vehicle_lat")).as("DB vehicle_lat")
				.isEqualByComparingTo(new BigDecimal("37.499456"));
		assertThat((BigDecimal) row.get("vehicle_lng")).as("DB vehicle_lng")
				.isEqualByComparingTo(new BigDecimal("127.028789"));
		PickupSession loaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();
		assertLatLng(loaded.getVehicleLocation(), NEWER_VEHICLE_LOCATION, "vehicleLocation");
		assertThat(loaded.getVehicleLocatedAt()).isEqualTo(T0.plusSeconds(15));
		assertThat(jdbcTemplate.queryForObject("select count(*) from pickup_session where vehicle_id = ?",
				Integer.class, seeded.vehicleId())).as("시드 차량 세션 행 수").isEqualTo(1);
	}

	@Test
	@DisplayName("경로를 totalMeters 4200으로 확정한 세션을 저장하면 route_total_meters 컬럼에 4200이 담기고 load로 읽으면 routeTotalMeters가 4200이다")
	void routeTotalMetersIsStoredInRouteTotalMetersColumnAndLoaded() {
		PickupSession session = seededSession(SessionStatus.ACTIVE);
		session.confirmRoute(4_200);

		saveSessionPort.save(session);

		assertThat(jdbcTemplate.queryForObject(
				"select route_total_meters from pickup_session where session_id = ?", Integer.class,
				seeded.sessionId())).as("DB route_total_meters").isEqualTo(4_200);
		PickupSession loaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();
		assertThat(loaded.getRouteTotalMeters()).isEqualTo(4_200);
	}

	@Test
	@DisplayName("경로를 확정하지 않은 세션을 저장하면 route_total_meters가 NULL이고 load로 읽으면 routeTotalMeters가 null이다")
	void sessionWithoutRouteTotalMetersStoresNullColumnAndLoadsNull() {
		saveSessionPort.save(seededSession(SessionStatus.ACTIVE));

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select route_total_meters from pickup_session where session_id = ?", seeded.sessionId());
		assertThat(row.get("route_total_meters")).as("DB route_total_meters").isNull();
		PickupSession loaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();
		assertThat(loaded.getRouteTotalMeters()).isNull();
	}

	@Test
	@DisplayName("totalMeters 4200으로 확정해 저장한 세션을 다시 읽어 3100으로 다시 확정해 저장하면 route_total_meters가 3100으로 바뀐다")
	void resavingSessionOverwritesRouteTotalMetersColumn() {
		PickupSession session = seededSession(SessionStatus.ACTIVE);
		session.confirmRoute(4_200);
		saveSessionPort.save(session);
		PickupSession reloaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();

		reloaded.confirmRoute(3_100);
		saveSessionPort.save(reloaded);

		assertThat(jdbcTemplate.queryForObject(
				"select route_total_meters from pickup_session where session_id = ?", Integer.class,
				seeded.sessionId())).as("DB route_total_meters").isEqualTo(3_100);
		assertThat(loadSessionPort.load(seeded.sessionId()).orElseThrow().getRouteTotalMeters()).isEqualTo(3_100);
	}

	@Test
	@DisplayName("같은 세션에 같은 childUserId의 SessionChild를 두 번 저장하면 (sessionId, childUserId) 유니크 위반 DataIntegrityViolationException이 나고 그 자녀 행은 1건만 남는다")
	void savingSameChildUserIdTwiceInOneSessionViolatesUniqueConstraint() {
		PickupSession seededWithDuplicate = PickupSession.restore(seeded.sessionId(), seeded.vehicleId(),
				seeded.familyId(), seeded.spotId(), PickupSeed.SPOT_NAME, SEED_STOP, SEED_MEETING, seeded.date(),
				SessionStatus.ACTIVE, VehiclePhase.ROUTING, SpotStatus.SELECTED, PickupSeed.STATE_VERSION, 0, 0, null,
				false, null, null, null, null, null, null, null, null, null, null, null, List.of(
						seededChild(seeded.minjunSessionChildId(), seeded.minjunId(), seeded.minjunScheduleId()),
						seededChild(seeded.seoyunSessionChildId(), seeded.seoyunId(), seeded.seoyunScheduleId()),
						SessionChild.waiting(seeded.minjunId(), seeded.seoyunScheduleId())));

		Throwable thrown = catchThrowable(() -> saveSessionPort.save(seededWithDuplicate));

		assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
		ConstraintViolationException violation = constraintViolationIn(thrown);
		assertThat(violation).as("원인에 Hibernate ConstraintViolationException이 있다").isNotNull();
		assertThat(violation.getKind()).isEqualTo(ConstraintKind.UNIQUE);
		assertThat(violation.getConstraintName()).isEqualToIgnoringCase(UNIQUE_CHILD_PER_SESSION);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from session_child where session_id = ? and child_user_id = ?", Integer.class,
				seeded.sessionId(), seeded.minjunId())).as("시드 세션의 민준 행 수").isEqualTo(1);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("loadActiveByVehicle은 그 차량의 ACTIVE · ON_HOLD 세션을 자녀와 함께 찾고 같은 차량의 COMPLETED · CANCELLED 세션과 다른 차량 세션은 돌려주지 않는다")
	void loadActiveByVehicleFindsInProgressSessionOfThatVehicleOnly(SessionStatus inProgress) {
		changeStatus(seeded.sessionId(), inProgress);
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().minusDays(1),
				SessionStatus.COMPLETED, List.of(minjunSchedule()));
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().minusDays(2),
				SessionStatus.CANCELLED, List.of(minjunSchedule()));
		UserSeed.Result other = seedOtherFamily();
		long otherSessionId = persistOtherFamilySession(other, SessionStatus.ACTIVE);

		PickupSession found = loadSessionPort.loadActiveByVehicle(seeded.vehicleId()).orElseThrow();

		assertThat(found.getSessionId()).isEqualTo(seeded.sessionId());
		assertThat(found.getVehicleId()).isEqualTo(seeded.vehicleId());
		assertThat(found.getSessionStatus()).isEqualTo(inProgress);
		assertThat(found.getChildren()).extracting(SessionChild::getChildUserId)
				.containsExactlyInAnyOrder(seeded.minjunId(), seeded.seoyunId());
		assertThat(loadSessionPort.loadActiveByVehicle(other.vehicleId())).as("다른 차량")
				.map(PickupSession::getSessionId)
				.contains(otherSessionId);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("loadActiveByVehicle은 그 차량에 COMPLETED · CANCELLED 세션만 있거나 세션이 없으면 빈 값을 돌려준다")
	void loadActiveByVehicleIsEmptyWhenVehicleHasNoInProgressSession(SessionStatus finished) {
		changeStatus(seeded.sessionId(), finished);
		UserSeed.Result other = seedOtherFamily();

		assertThat(loadSessionPort.loadActiveByVehicle(seeded.vehicleId())).as("끝난 세션만 있는 차량").isEmpty();
		assertThat(loadSessionPort.loadActiveByVehicle(other.vehicleId())).as("세션이 없는 차량").isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("loadActiveByChild는 그 자녀가 포함된 ACTIVE · ON_HOLD 세션을 형제와 함께 찾고, 그 자녀의 끝난 세션과 그 자녀가 포함되지 않은 세션은 돌려주지 않는다")
	void loadActiveByChildFindsInProgressSessionIncludingThatChild(SessionStatus inProgress) {
		changeStatus(seeded.sessionId(), inProgress);
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().minusDays(1),
				SessionStatus.COMPLETED, List.of(minjunSchedule()));
		UserSeed.Result other = seedOtherFamily();
		long otherSessionId = persistOtherFamilySession(other, SessionStatus.ACTIVE);

		PickupSession foundByMinjun = loadSessionPort.loadActiveByChild(seeded.minjunId()).orElseThrow();
		PickupSession foundBySeoyun = loadSessionPort.loadActiveByChild(seeded.seoyunId()).orElseThrow();

		assertThat(foundByMinjun.getSessionId()).isEqualTo(seeded.sessionId());
		assertThat(foundByMinjun.getSessionStatus()).isEqualTo(inProgress);
		assertThat(foundByMinjun.getChildren()).extracting(SessionChild::getChildUserId)
				.as("그 자녀만이 아니라 세션의 자녀 전체")
				.containsExactlyInAnyOrder(seeded.minjunId(), seeded.seoyunId());
		assertThat(foundBySeoyun.getSessionId()).isEqualTo(seeded.sessionId());
		assertThat(loadSessionPort.loadActiveByChild(other.minjunId())).as("다른 가족 세션에 포함된 자녀")
				.map(PickupSession::getSessionId)
				.contains(otherSessionId);
		assertThat(loadSessionPort.loadActiveByChild(other.seoyunId()))
				.as("가족에 진행 중 세션이 있지만 그 세션에 포함되지 않은 자녀")
				.isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("loadActiveByChild는 그 자녀가 COMPLETED · CANCELLED 세션에만 포함돼 있으면 빈 값을 돌려준다")
	void loadActiveByChildIsEmptyWhenChildIsOnlyInFinishedSessions(SessionStatus finished) {
		changeStatus(seeded.sessionId(), finished);

		assertThat(loadSessionPort.loadActiveByChild(seeded.minjunId())).as("민준").isEmpty();
		assertThat(loadSessionPort.loadActiveByChild(seeded.seoyunId())).as("서윤").isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("loadActiveByFamily는 그 가족의 ACTIVE · ON_HOLD 세션을 자녀와 함께 찾고 같은 가족의 끝난 세션과 다른 가족 세션은 돌려주지 않는다")
	void loadActiveByFamilyFindsInProgressSessionOfThatFamilyOnly(SessionStatus inProgress) {
		changeStatus(seeded.sessionId(), inProgress);
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().minusDays(1),
				SessionStatus.COMPLETED, List.of(minjunSchedule()));
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().minusDays(2),
				SessionStatus.CANCELLED, List.of(minjunSchedule()));
		UserSeed.Result other = seedOtherFamily();
		long otherSessionId = persistOtherFamilySession(other, SessionStatus.ON_HOLD);

		PickupSession found = loadSessionPort.loadActiveByFamily(seeded.familyId()).orElseThrow();

		assertThat(found.getSessionId()).isEqualTo(seeded.sessionId());
		assertThat(found.getFamilyId()).isEqualTo(seeded.familyId());
		assertThat(found.getSessionStatus()).isEqualTo(inProgress);
		assertThat(found.getChildren()).extracting(SessionChild::getChildUserId)
				.containsExactlyInAnyOrder(seeded.minjunId(), seeded.seoyunId());
		assertThat(loadSessionPort.loadActiveByFamily(other.familyId())).as("다른 가족")
				.map(PickupSession::getSessionId)
				.contains(otherSessionId);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("loadActiveByFamily는 그 가족에 COMPLETED · CANCELLED 세션만 있거나 세션이 없으면 빈 값을 돌려준다")
	void loadActiveByFamilyIsEmptyWhenFamilyHasNoInProgressSession(SessionStatus finished) {
		changeStatus(seeded.sessionId(), finished);
		UserSeed.Result other = seedOtherFamily();

		assertThat(loadSessionPort.loadActiveByFamily(seeded.familyId())).as("끝난 세션만 있는 가족").isEmpty();
		assertThat(loadSessionPort.loadActiveByFamily(other.familyId())).as("세션이 없는 가족").isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("loadActiveBySpot은 그 장소의 ACTIVE · ON_HOLD 세션을 찾고 같은 장소의 끝난 세션은 돌려주지 않으며 끝난 세션만 있는 다른 장소는 빈 값이다")
	void loadActiveBySpotFindsInProgressSessionAtThatSpotOnly(SessionStatus inProgress) {
		changeStatus(seeded.sessionId(), inProgress);
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().minusDays(1),
				SessionStatus.COMPLETED, List.of(minjunSchedule()));
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.otherSpotId(), seeded.date().minusDays(2),
				SessionStatus.CANCELLED, List.of(minjunSchedule()));

		PickupSession found = loadSessionPort.loadActiveBySpot(seeded.spotId()).orElseThrow();

		assertThat(found.getSessionId()).isEqualTo(seeded.sessionId());
		assertThat(found.getSpotId()).isEqualTo(seeded.spotId());
		assertThat(found.getSessionStatus()).isEqualTo(inProgress);
		assertThat(loadSessionPort.loadActiveBySpot(seeded.otherSpotId())).as("끝난 세션만 있는 다른 장소").isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("loadActiveBySpot은 그 장소에 COMPLETED · CANCELLED 세션만 있거나 세션이 없으면 빈 값을 돌려준다")
	void loadActiveBySpotIsEmptyWhenSpotHasNoInProgressSession(SessionStatus finished) {
		changeStatus(seeded.sessionId(), finished);

		assertThat(loadSessionPort.loadActiveBySpot(seeded.spotId())).as("끝난 세션만 있는 장소").isEmpty();
		assertThat(loadSessionPort.loadActiveBySpot(seeded.otherSpotId())).as("세션이 없는 장소").isEmpty();
	}

	@Test
	@DisplayName("loadByFamilyAndDate는 그 가족 · 그 날짜의 세션을 상태와 관계없이(ON_HOLD · COMPLETED · CANCELLED) 자녀(childUserId · scheduleId)와 함께 모두 돌려주고 다른 날짜 · 다른 가족 세션은 돌려주지 않는다")
	void loadByFamilyAndDateReturnsSessionsOfThatFamilyAndDateInAnyStatusWithChildren() {
		changeStatus(seeded.sessionId(), SessionStatus.ON_HOLD);
		long completedId = persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date(),
				SessionStatus.COMPLETED, List.of(minjunSchedule()));
		long cancelledId = persistSession(seeded.vehicleId(), seeded.familyId(), seeded.otherSpotId(), seeded.date(),
				SessionStatus.CANCELLED, List.of(new ChildSchedule(seeded.seoyunId(), seeded.seoyunScheduleId())));
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().minusDays(1),
				SessionStatus.COMPLETED, List.of(minjunSchedule()));
		persistSession(seeded.vehicleId(), seeded.familyId(), seeded.spotId(), seeded.date().plusDays(1),
				SessionStatus.ACTIVE, List.of(minjunSchedule()));
		UserSeed.Result other = seedOtherFamily();
		long otherSessionId = persistOtherFamilySession(other, SessionStatus.ACTIVE);

		List<PickupSession> found = loadSessionPort.loadByFamilyAndDate(seeded.familyId(), seeded.date());

		assertThat(found).extracting(PickupSession::getSessionId, PickupSession::getFamilyId, PickupSession::getDate,
						PickupSession::getSessionStatus)
				.containsExactlyInAnyOrder(
						tuple(seeded.sessionId(), seeded.familyId(), seeded.date(), SessionStatus.ON_HOLD),
						tuple(completedId, seeded.familyId(), seeded.date(), SessionStatus.COMPLETED),
						tuple(cancelledId, seeded.familyId(), seeded.date(), SessionStatus.CANCELLED));
		assertThat(sessionOf(found, seeded.sessionId()).getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getScheduleId)
				.containsExactlyInAnyOrder(
						tuple(seeded.minjunId(), seeded.minjunScheduleId()),
						tuple(seeded.seoyunId(), seeded.seoyunScheduleId()));
		assertThat(sessionOf(found, completedId).getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getScheduleId)
				.containsExactly(tuple(seeded.minjunId(), seeded.minjunScheduleId()));
		assertThat(sessionOf(found, cancelledId).getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getScheduleId)
				.containsExactly(tuple(seeded.seoyunId(), seeded.seoyunScheduleId()));
		assertThat(loadSessionPort.loadByFamilyAndDate(other.familyId(), seeded.date())).as("다른 가족")
				.extracting(PickupSession::getSessionId)
				.containsExactly(otherSessionId);
	}

	@Test
	@DisplayName("loadByFamilyAndDate는 그 가족에 그 날짜 세션이 없으면 빈 목록을 돌려준다")
	void loadByFamilyAndDateIsEmptyWhenFamilyHasNoSessionOnThatDate() {
		UserSeed.Result other = seedOtherFamily();

		assertThat(loadSessionPort.loadByFamilyAndDate(seeded.familyId(), seeded.date().plusDays(3)))
				.as("세션이 없는 날짜").isNotNull().isEmpty();
		assertThat(loadSessionPort.loadByFamilyAndDate(other.familyId(), seeded.date()))
				.as("세션이 없는 가족").isNotNull().isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(SessionStatus.class)
	@DisplayName("load는 상태와 관계없이 그 sessionId의 세션을 시드 스냅샷 · 자녀 2명(sessionChildId · scheduleId · WAITING)과 함께 돌려준다")
	void loadReturnsSessionWithChildrenInAnyStatus(SessionStatus status) {
		changeStatus(seeded.sessionId(), status);

		PickupSession loaded = loadSessionPort.load(seeded.sessionId()).orElseThrow();

		assertThat(loaded).usingRecursiveComparison()
				.ignoringFields("stopPoint", "meetingPoint", "children")
				.isEqualTo(seededSession(status));
		assertLatLng(loaded.getStopPoint(), SEED_STOP, "stopPoint");
		assertLatLng(loaded.getMeetingPoint(), SEED_MEETING, "meetingPoint");
		assertThat(loaded.getChildren())
				.extracting(SessionChild::getSessionChildId, SessionChild::getChildUserId, SessionChild::getScheduleId,
						SessionChild::getChildPhase, SessionChild::getExtraPrepMinutes, SessionChild::getLastLocation)
				.containsExactlyInAnyOrder(
						tuple(seeded.minjunSessionChildId(), seeded.minjunId(), seeded.minjunScheduleId(),
								ChildPhase.WAITING, 0, null),
						tuple(seeded.seoyunSessionChildId(), seeded.seoyunId(), seeded.seoyunScheduleId(),
								ChildPhase.WAITING, 0, null));
	}

	@Test
	@DisplayName("load는 없는 sessionId면 빈 값을 돌려준다")
	void loadIsEmptyForUnknownSessionId() {
		long unknownSessionId = jdbcTemplate.queryForObject(
				"select coalesce(max(session_id), 0) + 1000 from pickup_session", Long.class);

		assertThat(loadSessionPort.load(unknownSessionId)).isEmpty();
	}

	@Test
	@DisplayName("loadForUpdate로 세션을 잠근 트랜잭션이 끝나기 전에는 다른 트랜잭션의 loadForUpdate가 끝나지 않고, 앞 트랜잭션이 커밋한 뒤에 끝나며 앞 트랜잭션이 커밋한 stateVersion을 읽는다")
	void loadForUpdateWaitsUntilLockingTransactionEnds() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CompletableFuture<PickupSession> lockedByFirst = new CompletableFuture<>();
		try {
			CompletableFuture<Void> first = CompletableFuture.runAsync(
					() -> transactionTemplate.executeWithoutResult(tx -> {
						PickupSession locked = loadSessionPort.loadForUpdate(seeded.sessionId()).orElseThrow();
						jdbcTemplate.update("update pickup_session set state_version = ? where session_id = ?",
								STATE_VERSION_COMMITTED_BY_FIRST, seeded.sessionId());
						lockedByFirst.complete(locked);
						awaitRelease(releaseFirst);
					}), executor);
			first.whenComplete((ignored, error) -> {
				if (error != null) {
					lockedByFirst.completeExceptionally(error);
				}
			});

			PickupSession firstLoaded = lockedByFirst.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
			assertThat(firstLoaded.getSessionId()).isEqualTo(seeded.sessionId());
			assertThat(firstLoaded.getStateVersion()).as("앞 트랜잭션이 읽은 stateVersion").isEqualTo(PickupSeed.STATE_VERSION);
			assertThat(firstLoaded.getChildren()).extracting(SessionChild::getChildUserId)
					.containsExactlyInAnyOrder(seeded.minjunId(), seeded.seoyunId());

			CompletableFuture<PickupSession> second = CompletableFuture.supplyAsync(
					() -> transactionTemplate.execute(
							tx -> loadSessionPort.loadForUpdate(seeded.sessionId()).orElseThrow()), executor);

			Throwable whileLocked = catchThrowable(() -> second.get(STILL_WAITING_CHECK_MILLIS, TimeUnit.MILLISECONDS));
			assertThat(whileLocked).as("앞 트랜잭션이 잠근 동안 뒤 트랜잭션의 loadForUpdate는 끝나지 않고 기다린다")
					.isInstanceOf(TimeoutException.class);

			releaseFirst.countDown();
			first.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
			PickupSession secondLoaded = second.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);

			assertThat(secondLoaded.getSessionId()).isEqualTo(seeded.sessionId());
			assertThat(secondLoaded.getStateVersion()).as("뒤 트랜잭션은 앞 트랜잭션이 커밋한 값을 읽는다")
					.isEqualTo(STATE_VERSION_COMMITTED_BY_FIRST);
		} finally {
			releaseFirst.countDown();
			executor.shutdown();
			executor.awaitTermination(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
		}
	}

	@Test
	@DisplayName("loadBoardingExpired는 boardingCancelableUntil이 now보다 앞선 ACTIVE 세션을 자녀와 함께 한 번씩 돌려주고, boardingCancelableUntil이 아직 지나지 않았거나 비어 있는 ACTIVE 세션은 돌려주지 않는다")
	void loadBoardingExpiredReturnsActiveSessionsWhoseCancelWindowHasPassed() {
		markBoarding(seeded.sessionId(), T0.minusSeconds(1));
		long windowOpenSessionId = persistOtherFamilySession(seedOtherFamily(), SessionStatus.ACTIVE);
		markBoarding(windowOpenSessionId, T0.plusSeconds(5));
		long notBoardedSessionId = persistOtherFamilySession(seedOtherFamily(), SessionStatus.ACTIVE);

		List<PickupSession> expired = loadSessionPort.loadBoardingExpired(T0);

		assertThat(expired).extracting(PickupSession::getSessionId)
				.as("다른 테스트가 남긴 세션이 섞일 수 있어 이 테스트가 만든 세션만 본다")
				.containsOnlyOnce(seeded.sessionId())
				.doesNotContain(windowOpenSessionId, notBoardedSessionId);
		PickupSession found = expired.stream()
				.filter(session -> session.getSessionId() == seeded.sessionId())
				.findFirst()
				.orElseThrow();
		assertThat(found.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(found.getBoardingCancelableUntil()).isEqualTo(T0.minusSeconds(1));
		assertThat(found.getChildren()).extracting(SessionChild::getChildUserId)
				.as("자동 완료가 자녀 탑승을 확인할 수 있게 자녀 전체")
				.containsExactlyInAnyOrder(seeded.minjunId(), seeded.seoyunId());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"ON_HOLD", "COMPLETED", "CANCELLED"})
	@DisplayName("loadBoardingExpired는 boardingCancelableUntil이 지났어도 ON_HOLD · COMPLETED · CANCELLED 세션은 돌려주지 않는다")
	void loadBoardingExpiredSkipsSessionsThatAreNotActive(SessionStatus notActive) {
		changeStatus(seeded.sessionId(), notActive);
		markBoarding(seeded.sessionId(), T0.minusSeconds(1));
		long activeSessionId = persistOtherFamilySession(seedOtherFamily(), SessionStatus.ACTIVE);
		markBoarding(activeSessionId, T0.minusSeconds(1));

		List<PickupSession> expired = loadSessionPort.loadBoardingExpired(T0);

		assertThat(expired).extracting(PickupSession::getSessionId)
				.as("같은 시각에 창이 지난 ACTIVE 세션은 돌려주고 " + notActive + " 세션은 돌려주지 않는다")
				.contains(activeSessionId)
				.doesNotContain(seeded.sessionId());
	}

	private PickupSession seededSession(SessionStatus status) {
		return PickupSession.restore(seeded.sessionId(), seeded.vehicleId(), seeded.familyId(), seeded.spotId(),
				PickupSeed.SPOT_NAME, SEED_STOP, SEED_MEETING, seeded.date(), status, VehiclePhase.ROUTING,
				SpotStatus.SELECTED, PickupSeed.STATE_VERSION, 0, 0, null, false, null, null, null, null, null, null,
				null, null, null, null, null, List.of(
						seededChild(seeded.minjunSessionChildId(), seeded.minjunId(), seeded.minjunScheduleId()),
						seededChild(seeded.seoyunSessionChildId(), seeded.seoyunId(), seeded.seoyunScheduleId())));
	}

	private static SessionChild seededChild(long sessionChildId, long childUserId, long scheduleId) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.WAITING, 0, null, null, null,
				null, null);
	}

	private ChildSchedule minjunSchedule() {
		return new ChildSchedule(seeded.minjunId(), seeded.minjunScheduleId());
	}

	/** 다른 가족 차량 · 민준 이름의 자녀 1명만 포함된 오늘 세션. DB 외래 키가 없어 장소 · 일정 ID는 시드 값을 빌려 쓴다. */
	private long persistOtherFamilySession(UserSeed.Result other, SessionStatus status) {
		return persistSession(other.vehicleId(), other.familyId(), seeded.otherSpotId(), seeded.date(), status,
				List.of(new ChildSchedule(other.minjunId(), seeded.minjunScheduleId())));
	}

	private long persistSession(long vehicleId, long familyId, long spotId, LocalDate date, SessionStatus status,
			List<ChildSchedule> children) {
		return transactionTemplate.execute(tx -> {
			PickupSessionEntity session = new PickupSessionEntity(null, vehicleId, familyId, spotId,
					PickupSeed.SPOT_NAME, GeoPoints.toPoint(PickupSeed.STOP_LAT, PickupSeed.STOP_LNG),
					GeoPoints.toPoint(PickupSeed.MEETING_LAT, PickupSeed.MEETING_LNG), date, status,
					VehiclePhase.ROUTING, SpotStatus.SELECTED, 0, 0, 0, null, false, null, null, null, null, null,
					null, null, null, null, null, null);
			entityManager.persist(session);
			for (ChildSchedule child : children) {
				entityManager.persist(new SessionChildEntity(null, session.getSessionId(), child.childUserId(),
						child.scheduleId(), ChildPhase.WAITING, (short) 0, null, null, null, null, null, null));
			}
			entityManager.flush();
			return session.getSessionId();
		});
	}

	private void changeStatus(long sessionId, SessionStatus status) {
		jdbcTemplate.update("update pickup_session set session_status = ? where session_id = ?", status.name(),
				sessionId);
	}

	/** 탑승 감지 상태로 만든다: boardingDetectedAt = cancelableUntil - 10초. */
	private void markBoarding(long sessionId, Instant cancelableUntil) {
		jdbcTemplate.update("""
						update pickup_session
						set boarding_detected_at = ?, boarding_cancelable_until = ?
						where session_id = ?
						""", cancelableUntil.minusSeconds(BOARDING_WINDOW_SECONDS).atOffset(ZoneOffset.UTC),
				cancelableUntil.atOffset(ZoneOffset.UTC), sessionId);
	}

	private static void awaitRelease(CountDownLatch release) {
		try {
			if (!release.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("잠금을 풀라는 신호가 " + MAX_WAIT_SECONDS + "초 안에 오지 않았다");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private UserSeed.Result seedOtherFamily() {
		UserSeed.Result family = userSeed.seed();
		otherFamilies.add(family);
		return family;
	}

	private static SessionChild childOf(PickupSession session, long childUserId) {
		return session.getChildren().stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.findFirst()
				.orElseThrow();
	}

	private static PickupSession sessionOf(List<PickupSession> sessions, long sessionId) {
		return sessions.stream()
				.filter(session -> session.getSessionId() == sessionId)
				.findFirst()
				.orElseThrow();
	}

	private static ConstraintViolationException constraintViolationIn(Throwable thrown) {
		for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation;
			}
		}
		return null;
	}

	private static void assertLatLng(LatLng actual, LatLng expected, String name) {
		assertThat(actual).as(name).isNotNull();
		assertThat(actual.lat()).as(name + " lat").isCloseTo(expected.lat(), within(SIX_DECIMALS));
		assertThat(actual.lng()).as(name + " lng").isCloseTo(expected.lng(), within(SIX_DECIMALS));
	}

}

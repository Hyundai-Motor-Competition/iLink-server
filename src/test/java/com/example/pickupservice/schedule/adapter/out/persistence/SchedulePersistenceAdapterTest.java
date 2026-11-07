package com.example.pickupservice.schedule.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SaveAcademyPort;
import com.example.pickupservice.schedule.application.port.out.SaveSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import(UserSeed.class)
class SchedulePersistenceAdapterTest extends IntegrationTestSupport {

	private static final double COORDINATE_TOLERANCE = 1e-9;
	private static final LatLng ACADEMY_LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ACADEMY_ENTRANCE = new LatLng(37.501150, 127.039790);
	private static final LocalTime SIX_PM = LocalTime.of(18, 0);
	private static final String FAR_EAST_ZONE = "Pacific/Kiritimati";

	private final SaveAcademyPort saveAcademyPort;
	private final LoadAcademyPort loadAcademyPort;
	private final SaveSchedulePort saveSchedulePort;
	private final LoadSchedulePort loadSchedulePort;
	private final UserSeed userSeed;
	private final EntityManager entityManager;
	private final EntityManagerFactory entityManagerFactory;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private final List<Long> academyIds = new ArrayList<>();
	private final List<Long> scheduleIds = new ArrayList<>();
	private UserSeed.Result users;

	@BeforeEach
	void seedUsers() {
		users = userSeed.seed();
	}

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> {
			if (!scheduleIds.isEmpty()) {
				entityManager.createQuery("delete from PickupScheduleEntity s where s.scheduleId in :ids")
						.setParameter("ids", scheduleIds)
						.executeUpdate();
			}
			if (!academyIds.isEmpty()) {
				entityManager.createQuery("delete from AcademyEntity a where a.academyId in :ids")
						.setParameter("ids", academyIds)
						.executeUpdate();
			}
		});
		userSeed.clear(users);
	}

	@Test
	@DisplayName("Academy를 저장하고 읽으면 location · entrance 좌표가 같다")
	void savedAcademyIsLoadedWithSameLocationAndEntrance() {
		Academy saved = saveAcademyPort.saveAcademy(Academy.create(uniquePlaceId(), "해솔영어학원", "서울 강남구 테헤란로 152",
				ACADEMY_LOCATION, ACADEMY_ENTRANCE, "02-555-1234"));
		assertThat(saved.getAcademyId()).as("저장된 academyId").isNotNull();
		academyIds.add(saved.getAcademyId());
		Academy savedWithoutEntrance = saveAcademyPort.saveAcademy(Academy.create(uniquePlaceId(), "바른수학학원",
				"서울 강남구 역삼로 10", ACADEMY_LOCATION, null, null));
		academyIds.add(savedWithoutEntrance.getAcademyId());
		clearPersistenceContext();

		Academy loaded = loadAcademyPort.loadAcademy(saved.getAcademyId()).orElseThrow();
		assertThat(loaded.getAcademyId()).isEqualTo(saved.getAcademyId());
		assertThat(loaded.getName()).isEqualTo("해솔영어학원");
		assertThat(loaded.getAddress()).isEqualTo("서울 강남구 테헤란로 152");
		assertThat(loaded.getPhone()).isEqualTo("02-555-1234");
		assertThat(loaded.getLocation().lat()).as("location 위도").isCloseTo(ACADEMY_LOCATION.lat(),
				within(COORDINATE_TOLERANCE));
		assertThat(loaded.getLocation().lng()).as("location 경도").isCloseTo(ACADEMY_LOCATION.lng(),
				within(COORDINATE_TOLERANCE));
		assertThat(loaded.getEntrance()).as("entrance").isNotNull();
		assertThat(loaded.getEntrance().lat()).as("entrance 위도").isCloseTo(ACADEMY_ENTRANCE.lat(),
				within(COORDINATE_TOLERANCE));
		assertThat(loaded.getEntrance().lng()).as("entrance 경도").isCloseTo(ACADEMY_ENTRANCE.lng(),
				within(COORDINATE_TOLERANCE));

		Academy loadedWithoutEntrance = loadAcademyPort.loadAcademy(savedWithoutEntrance.getAcademyId())
				.orElseThrow();
		assertThat(loadedWithoutEntrance.getEntrance()).as("출입구 없이 저장한 학원의 entrance").isNull();
		assertThat(loadedWithoutEntrance.getPhone()).as("대표번호 없이 저장한 학원의 phone").isNull();
	}

	@Test
	@DisplayName("Academy를 저장하면 external_place_id 컬럼에 externalPlaceId가 들어가고 loadAcademy · loadAcademyByExternalPlaceId로 읽으면 같은 학원이 나온다")
	void savedAcademyKeepsExternalPlaceIdAndIsFoundByIt() {
		String placeId = uniquePlaceId();
		Academy saved = saveAcademyPort.saveAcademy(Academy.create(placeId, "해솔영어학원", "서울 강남구 테헤란로 152",
				ACADEMY_LOCATION, ACADEMY_ENTRANCE, "02-555-1234"));
		academyIds.add(saved.getAcademyId());
		Academy other = saveAcademyPort.saveAcademy(Academy.create(uniquePlaceId(), "바른수학학원", "서울 강남구 역삼로 10",
				ACADEMY_LOCATION, null, null));
		academyIds.add(other.getAcademyId());
		clearPersistenceContext();

		String column = jdbcTemplate.queryForObject(
				"select external_place_id from academy where academy_id = ?", String.class, saved.getAcademyId());
		assertThat(column).as("external_place_id 컬럼 원문").isEqualTo(placeId);
		assertThat(saved.getExternalPlaceId()).as("saveAcademy가 돌려준 학원의 externalPlaceId").isEqualTo(placeId);
		assertThat(loadAcademyPort.loadAcademy(saved.getAcademyId()).orElseThrow().getExternalPlaceId())
				.as("loadAcademy로 읽은 externalPlaceId")
				.isEqualTo(placeId);

		Academy found = loadAcademyPort.loadAcademyByExternalPlaceId(placeId).orElseThrow();
		assertThat(found.getAcademyId()).as("externalPlaceId로 찾은 academyId").isEqualTo(saved.getAcademyId());
		assertThat(found.getExternalPlaceId()).isEqualTo(placeId);
		assertThat(found.getName()).isEqualTo("해솔영어학원");
		assertThat(found.getAddress()).isEqualTo("서울 강남구 테헤란로 152");
		assertThat(found.getPhone()).isEqualTo("02-555-1234");
		assertThat(found.getLocation().lat()).isCloseTo(ACADEMY_LOCATION.lat(), within(COORDINATE_TOLERANCE));
		assertThat(found.getLocation().lng()).isCloseTo(ACADEMY_LOCATION.lng(), within(COORDINATE_TOLERANCE));
		assertThat(found.getEntrance()).as("entrance").isNotNull();
		assertThat(found.getEntrance().lat()).isCloseTo(ACADEMY_ENTRANCE.lat(), within(COORDINATE_TOLERANCE));
		assertThat(found.getEntrance().lng()).isCloseTo(ACADEMY_ENTRANCE.lng(), within(COORDINATE_TOLERANCE));
	}

	@Test
	@DisplayName("loadAcademyByExternalPlaceId에 저장된 적 없는 값을 주면 빈 값을 돌려준다")
	void loadAcademyByExternalPlaceIdReturnsEmptyForUnknownValue() {
		newAcademy();
		clearPersistenceContext();

		assertThat(loadAcademyPort.loadAcademyByExternalPlaceId(uniquePlaceId())).as("없는 externalPlaceId").isEmpty();
	}

	@Test
	@DisplayName("같은 externalPlaceId로 academyId 없는 학원을 한 번 더 저장하면 유니크 제약 위반으로 거절되고 행이 늘지 않는다")
	void savingAnotherAcademyWithSameExternalPlaceIdViolatesUniqueConstraint() {
		String placeId = uniquePlaceId();
		Academy first = saveAcademyPort.saveAcademy(Academy.create(placeId, "해솔영어학원", "서울 강남구 테헤란로 152",
				ACADEMY_LOCATION, ACADEMY_ENTRANCE, "02-555-1234"));
		academyIds.add(first.getAcademyId());
		clearPersistenceContext();

		assertThatThrownBy(() -> saveAcademyPort.saveAcademy(Academy.create(placeId, "해솔영어학원 2관",
				"서울 강남구 테헤란로 200", ACADEMY_LOCATION, null, null)))
				.isInstanceOf(DataIntegrityViolationException.class);

		Integer rows = jdbcTemplate.queryForObject(
				"select count(*) from academy where external_place_id = ?", Integer.class, placeId);
		assertThat(rows).as("externalPlaceId %s인 academy 행 수", placeId).isEqualTo(1);
	}

	@Test
	@DisplayName("saveAcademy로 academyId가 있는 학원을 다시 저장하면 새 행을 만들지 않고 그 행의 이름 · 주소 · 위치 · 출입구 · 전화가 바뀌며 externalPlaceId는 그대로다")
	void savingAcademyWithIdUpdatesSameRowInsteadOfInsertingNewOne() {
		String placeId = uniquePlaceId();
		Academy saved = saveAcademyPort.saveAcademy(Academy.create(placeId, "해솔영어학원", "서울 강남구 테헤란로 152",
				ACADEMY_LOCATION, null, null));
		academyIds.add(saved.getAcademyId());
		clearPersistenceContext();
		LatLng newLocation = new LatLng(37.497942, 127.027621);

		Academy updated = saveAcademyPort.saveAcademy(Academy.restore(saved.getAcademyId(), placeId, "해솔영어학원 본관",
				"서울 강남구 테헤란로 200", newLocation, ACADEMY_ENTRANCE, "02-555-9999"));
		clearPersistenceContext();

		assertThat(updated.getAcademyId()).as("다시 저장한 학원의 academyId").isEqualTo(saved.getAcademyId());
		Integer rows = jdbcTemplate.queryForObject(
				"select count(*) from academy where external_place_id = ?", Integer.class, placeId);
		assertThat(rows).as("externalPlaceId %s인 academy 행 수", placeId).isEqualTo(1);
		Academy loaded = loadAcademyPort.loadAcademy(saved.getAcademyId()).orElseThrow();
		assertThat(loaded.getExternalPlaceId()).isEqualTo(placeId);
		assertThat(loaded.getName()).isEqualTo("해솔영어학원 본관");
		assertThat(loaded.getAddress()).isEqualTo("서울 강남구 테헤란로 200");
		assertThat(loaded.getPhone()).isEqualTo("02-555-9999");
		assertThat(loaded.getLocation().lat()).isCloseTo(newLocation.lat(), within(COORDINATE_TOLERANCE));
		assertThat(loaded.getLocation().lng()).isCloseTo(newLocation.lng(), within(COORDINATE_TOLERANCE));
		assertThat(loaded.getEntrance()).as("새로 생긴 entrance").isNotNull();
		assertThat(loaded.getEntrance().lat()).isCloseTo(ACADEMY_ENTRANCE.lat(), within(COORDINATE_TOLERANCE));
	}

	@Test
	@DisplayName("weekdays [MON, WED, FRI]를 저장하면 컬럼에 MON,WED,FRI로 들어가고 같은 목록으로 읽힌다")
	void weekdaysAreStoredAsCommaSeparatedAbbreviationsAndLoadedBack() {
		Academy academy = newAcademy();
		PickupSchedule saved = saveSchedulePort.saveSchedule(PickupSchedule.create(users.minjunId(), academy,
				Set.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), SIX_PM, 5));
		assertThat(saved.getScheduleId()).as("저장된 scheduleId").isNotNull();
		scheduleIds.add(saved.getScheduleId());
		clearPersistenceContext();

		String column = jdbcTemplate.queryForObject(
				"select weekdays from pickup_schedule where schedule_id = ?", String.class, saved.getScheduleId());
		assertThat(column).as("weekdays 컬럼 원문").isEqualTo("MON,WED,FRI");

		PickupSchedule loaded = loadSchedulePort.loadSchedule(saved.getScheduleId()).orElseThrow();
		assertThat(loaded.getWeekdays())
				.containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
		assertThat(loaded.getChildUserId()).isEqualTo(users.minjunId());
		assertThat(loaded.getAcademy().getAcademyId()).isEqualTo(academy.getAcademyId());
		assertThat(loaded.getTargetPickup()).isEqualTo(5);
		assertThat(loaded.getStatus()).as("출입구가 있는 학원의 일정 status").isEqualTo(ScheduleStatus.READY);
	}

	@Test
	@DisplayName("saveSchedule로 scheduleId가 있는 일정을 다시 저장하면 새 행을 만들지 않고 그 행의 요일 · 시간 · 학원 · status가 바뀐다")
	void savingScheduleWithIdUpdatesSameRowInsteadOfInsertingNewOne() {
		Academy academy = newAcademy();
		PickupSchedule saved = newSchedule(users.minjunId(), academy, DayOfWeek.MONDAY);
		Academy academyWithoutEntrance = saveAcademyPort.saveAcademy(Academy.create(uniquePlaceId(), "바른수학학원",
				"서울 강남구 역삼로 10", ACADEMY_LOCATION, null, null));
		academyIds.add(academyWithoutEntrance.getAcademyId());
		clearPersistenceContext();
		Integer rowsBefore = jdbcTemplate.queryForObject(
				"select count(*) from pickup_schedule where child_user_id = ?", Integer.class, users.minjunId());

		PickupSchedule updated = saveSchedulePort.saveSchedule(PickupSchedule.restore(saved.getScheduleId(),
				users.minjunId(), academyWithoutEntrance, Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
				LocalTime.of(19, 30), 10, ScheduleStatus.DRAFT));
		clearPersistenceContext();

		assertThat(updated.getScheduleId()).as("다시 저장한 일정의 scheduleId").isEqualTo(saved.getScheduleId());
		Integer rowsAfter = jdbcTemplate.queryForObject(
				"select count(*) from pickup_schedule where child_user_id = ?", Integer.class, users.minjunId());
		assertThat(rowsAfter).as("민준의 일정 행 수").isEqualTo(rowsBefore).isEqualTo(1);
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select weekdays, class_end_time::text as class_end_time, academy_id, target_pickup, status"
						+ " from pickup_schedule where schedule_id = ?", saved.getScheduleId());
		assertThat(row.get("weekdays")).as("weekdays 컬럼").isEqualTo("TUE,THU");
		assertThat(row.get("class_end_time")).as("class_end_time 컬럼").isEqualTo("19:30:00");
		assertThat(((Number) row.get("academy_id")).longValue()).as("academy_id 컬럼")
				.isEqualTo(academyWithoutEntrance.getAcademyId());
		assertThat(((Number) row.get("target_pickup")).intValue()).as("target_pickup 컬럼").isEqualTo(10);
		assertThat(row.get("status")).as("status 컬럼").isEqualTo("DRAFT");
	}

	@Test
	@DisplayName("loadSchedule · loadAcademy에 없는 id를 주면 빈 값을 돌려준다")
	void loadScheduleAndLoadAcademyReturnEmptyForUnknownId() {
		Long maxScheduleId = jdbcTemplate.queryForObject(
				"select coalesce(max(schedule_id), 0) from pickup_schedule", Long.class);
		Long maxAcademyId = jdbcTemplate.queryForObject(
				"select coalesce(max(academy_id), 0) from academy", Long.class);

		assertThat(loadSchedulePort.loadSchedule(maxScheduleId + 1000)).as("없는 scheduleId").isEmpty();
		assertThat(loadAcademyPort.loadAcademy(maxAcademyId + 1000)).as("없는 academyId").isEmpty();
	}

	@Test
	@DisplayName("classEndTime 18:00을 저장하면 시간대 변환 없이 18:00으로 읽힌다")
	void classEndTimeIsLoadedWithoutTimeZoneConversion() {
		Academy academy = newAcademy();
		TimeZone original = TimeZone.getDefault();
		PickupSchedule loaded;
		long scheduleId;
		try {
			TimeZone.setDefault(TimeZone.getTimeZone(FAR_EAST_ZONE));
			PickupSchedule saved = saveSchedulePort.saveSchedule(PickupSchedule.create(users.minjunId(), academy,
					Set.of(DayOfWeek.TUESDAY), SIX_PM, 0));
			scheduleId = saved.getScheduleId();
			scheduleIds.add(scheduleId);
			clearPersistenceContext();
			loaded = loadSchedulePort.loadSchedule(scheduleId).orElseThrow();
		}
		finally {
			TimeZone.setDefault(original);
		}

		assertThat(loaded.getClassEndTime()).as("JVM 시간대가 UTC+14일 때 저장 · 조회한 classEndTime").isEqualTo(SIX_PM);
		String column = jdbcTemplate.queryForObject(
				"select class_end_time::text from pickup_schedule where schedule_id = ?", String.class, scheduleId);
		assertThat(column).as("class_end_time 컬럼 원문").isEqualTo("18:00:00");
		assertThat(loadSchedulePort.loadSchedule(scheduleId).orElseThrow().getClassEndTime())
				.as("기본 시간대로 다시 읽은 classEndTime")
				.isEqualTo(SIX_PM);
	}

	@Test
	@DisplayName("loadSchedulesOfChild는 그 자녀의 일정만 돌려준다")
	void loadSchedulesOfChildReturnsOnlySchedulesOfThatChild() {
		Academy academy = newAcademy();
		PickupSchedule minjunMonday = newSchedule(users.minjunId(), academy, DayOfWeek.MONDAY);
		PickupSchedule minjunThursday = newSchedule(users.minjunId(), academy, DayOfWeek.THURSDAY);
		newSchedule(users.seoyunId(), academy, DayOfWeek.MONDAY);
		clearPersistenceContext();

		List<PickupSchedule> loaded = loadSchedulePort.loadSchedulesOfChild(users.minjunId());

		assertThat(loaded).extracting(PickupSchedule::getScheduleId)
				.containsExactlyInAnyOrder(minjunMonday.getScheduleId(), minjunThursday.getScheduleId());
		assertThat(loaded).extracting(PickupSchedule::getChildUserId)
				.as("모두 민준의 일정")
				.containsOnly(users.minjunId());
		assertThat(loadSchedulePort.loadSchedulesOfChild(users.guardianId())).as("일정이 없는 사용자").isEmpty();
	}

	@Test
	@DisplayName("loadSchedulesOfChildren은 여러 자녀의 일정을 한 번에 돌려주고 목록에 없는 자녀의 일정은 빼며, 일정 조회 쿼리는 자녀 수와 상관없이 1회다")
	void loadSchedulesOfChildrenReturnsSchedulesOfAllGivenChildrenAtOnce() {
		Academy academy = newAcademy();
		PickupSchedule minjunMonday = newSchedule(users.minjunId(), academy, DayOfWeek.MONDAY);
		PickupSchedule minjunThursday = newSchedule(users.minjunId(), academy, DayOfWeek.THURSDAY);
		PickupSchedule seoyunTuesday = newSchedule(users.seoyunId(), academy, DayOfWeek.TUESDAY);
		newSchedule(users.guardianId(), academy, DayOfWeek.FRIDAY);
		clearPersistenceContext();

		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		boolean statisticsWasEnabled = statistics.isStatisticsEnabled();
		List<PickupSchedule> loaded;
		long statementCount;
		try {
			statistics.setStatisticsEnabled(true);
			statistics.clear();
			loaded = loadSchedulePort.loadSchedulesOfChildren(List.of(users.minjunId(), users.seoyunId()));
			statementCount = statistics.getPrepareStatementCount();
		}
		finally {
			statistics.setStatisticsEnabled(statisticsWasEnabled);
		}

		assertThat(loaded).extracting(PickupSchedule::getScheduleId)
				.containsExactlyInAnyOrder(minjunMonday.getScheduleId(), minjunThursday.getScheduleId(),
						seoyunTuesday.getScheduleId());
		assertThat(loaded).extracting(PickupSchedule::getChildUserId)
				.as("민준 · 서윤의 일정만")
				.containsOnly(users.minjunId(), users.seoyunId());
		assertThat(loaded).extracting(schedule -> schedule.getAcademy().getAcademyId())
				.as("일정마다 학원이 채워진다")
				.containsOnly(academy.getAcademyId());
		assertThat(statementCount).as("SQL 수 (일정 IN 조회 1회 + 학원 조회 1회 이하)").isLessThanOrEqualTo(2);
	}

	@Test
	@DisplayName("loadSchedulesOfChildren에 빈 목록을 주면 빈 목록을 돌려준다")
	void loadSchedulesOfChildrenReturnsEmptyListForEmptyInput() {
		Academy academy = newAcademy();
		newSchedule(users.minjunId(), academy, DayOfWeek.MONDAY);
		clearPersistenceContext();

		assertThat(loadSchedulePort.loadSchedulesOfChildren(List.of())).isNotNull().isEmpty();
		assertThat(loadSchedulePort.loadSchedulesOfChildren(List.of(users.guardianId())))
				.as("일정이 없는 사용자만 넘긴 경우")
				.isEmpty();
	}

	private Academy newAcademy() {
		Academy academy = saveAcademyPort.saveAcademy(Academy.create(uniquePlaceId(), "해솔영어학원", "서울 강남구 테헤란로 152",
				ACADEMY_LOCATION, ACADEMY_ENTRANCE, "02-555-1234"));
		academyIds.add(academy.getAcademyId());
		return academy;
	}

	private static String uniquePlaceId() {
		return "test:" + UUID.randomUUID();
	}

	private PickupSchedule newSchedule(long childUserId, Academy academy, DayOfWeek weekday) {
		PickupSchedule schedule = saveSchedulePort.saveSchedule(
				PickupSchedule.create(childUserId, academy, Set.of(weekday), SIX_PM, 5));
		scheduleIds.add(schedule.getScheduleId());
		return schedule;
	}

	private void clearPersistenceContext() {
		transactionTemplate.executeWithoutResult(status -> entityManager.clear());
	}

}

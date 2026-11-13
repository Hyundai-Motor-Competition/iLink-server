package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.fixture.GeoOffsets;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.DefaultSpotConflictException;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.adapter.out.persistence.AcademyEntity;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class})
class PlacePersistenceAdapterTest extends IntegrationTestSupport {

	/** 소수점 6자리까지 같다 = 차이가 0.0000005 미만. */
	private static final double SIX_DECIMALS = 0.0000005;
	private static final LatLng SAVED_STOP = new LatLng(37.501777, 127.039123);
	private static final LatLng SAVED_MEETING = new LatLng(37.501888, 127.039456);
	private static final LatLng OTHER_ACADEMY_LOCATION = new LatLng(37.497952, 127.027619);
	/** 반경 조회 기준점 = 시드 학원 위치 = 시드 기본 장소 정차 지점. 시드 다른 장소 정차 지점은 여기서 약 111m. */
	private static final LatLng NEAR = new LatLng(ScheduleSeed.ACADEMY_LAT, ScheduleSeed.ACADEMY_LNG);
	private static final int RADIUS_M = 500;

	private final LoadSpotPort loadSpotPort;
	private final SaveSpotPort saveSpotPort;
	private final PlaceSeed placeSeed;
	private final UserSeed userSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private final List<UserSeed.Result> otherFamilies = new ArrayList<>();
	private final List<Long> otherAcademyIds = new ArrayList<>();
	private PlaceSeed.Result seeded;
	private long familyId;
	private long academyId;
	private long guardianId;

	@BeforeEach
	void seed() {
		seeded = placeSeed.seed();
		familyId = seeded.schedule().user().familyId();
		academyId = seeded.schedule().academyId();
		guardianId = seeded.schedule().user().guardianId();
	}

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> {
			for (UserSeed.Result family : otherFamilies) {
				entityManager.createQuery("delete from PickupSpotEntity s where s.familyId = :familyId")
						.setParameter("familyId", family.familyId())
						.executeUpdate();
			}
			if (!otherAcademyIds.isEmpty()) {
				entityManager.createQuery("delete from AcademyEntity a where a.academyId in :ids")
						.setParameter("ids", otherAcademyIds)
						.executeUpdate();
			}
		});
		otherFamilies.forEach(userSeed::clear);
		placeSeed.clear(seeded);
	}

	@Test
	@DisplayName("PickupSpot을 저장하고 읽으면 stopPoint · meetingPoint의 lat · lng가 소수점 6자리까지 같다")
	void savedSpotIsLoadedWithStopAndMeetingPointEqualToSixDecimals() {
		PickupSpot saved = saveSpotPort.saveSpot(PickupSpot.create(familyId, academyId, "옆문 앞", "약국 앞",
				SAVED_STOP, SAVED_MEETING, false, guardianId));

		assertThat(saved.getSpotId()).as("저장하면 spotId가 생긴다").isNotNull();

		Map<String, Object> row = jdbcTemplate.queryForMap("""
				select family_id, academy_id, name, memo, is_default, created_by,
				       st_y(stop_point::geometry) as stop_lat, st_x(stop_point::geometry) as stop_lng,
				       st_y(meeting_point::geometry) as meeting_lat, st_x(meeting_point::geometry) as meeting_lng
				from pickup_spot
				where spot_id = ?
				""", saved.getSpotId());
		assertThat(((Number) row.get("family_id")).longValue()).isEqualTo(familyId);
		assertThat(((Number) row.get("academy_id")).longValue()).isEqualTo(academyId);
		assertThat(row.get("name")).isEqualTo("옆문 앞");
		assertThat(row.get("memo")).isEqualTo("약국 앞");
		assertThat(row.get("is_default")).isEqualTo(false);
		assertThat(((Number) row.get("created_by")).longValue()).isEqualTo(guardianId);
		assertThat((Double) row.get("stop_lat")).as("DB stop_point lat").isCloseTo(SAVED_STOP.lat(), within(SIX_DECIMALS));
		assertThat((Double) row.get("stop_lng")).as("DB stop_point lng").isCloseTo(SAVED_STOP.lng(), within(SIX_DECIMALS));
		assertThat((Double) row.get("meeting_lat")).as("DB meeting_point lat")
				.isCloseTo(SAVED_MEETING.lat(), within(SIX_DECIMALS));
		assertThat((Double) row.get("meeting_lng")).as("DB meeting_point lng")
				.isCloseTo(SAVED_MEETING.lng(), within(SIX_DECIMALS));

		PickupSpot loaded = loadSpotPort.loadSpot(saved.getSpotId()).orElseThrow();
		assertThat(loaded.getSpotId()).isEqualTo(saved.getSpotId());
		assertThat(loaded.getFamilyId()).isEqualTo(familyId);
		assertThat(loaded.getAcademyId()).isEqualTo(academyId);
		assertThat(loaded.getName()).isEqualTo("옆문 앞");
		assertThat(loaded.getMemo()).isEqualTo("약국 앞");
		assertThat(loaded.isDefault()).isFalse();
		assertThat(loaded.getCreatedBy()).isEqualTo(guardianId);
		assertThat(loaded.getStopPoint().lat()).as("stopPoint lat").isCloseTo(SAVED_STOP.lat(), within(SIX_DECIMALS));
		assertThat(loaded.getStopPoint().lng()).as("stopPoint lng").isCloseTo(SAVED_STOP.lng(), within(SIX_DECIMALS));
		assertThat(loaded.getMeetingPoint().lat()).as("meetingPoint lat")
				.isCloseTo(SAVED_MEETING.lat(), within(SIX_DECIMALS));
		assertThat(loaded.getMeetingPoint().lng()).as("meetingPoint lng")
				.isCloseTo(SAVED_MEETING.lng(), within(SIX_DECIMALS));
	}

	@Test
	@DisplayName("시드로 넣은 장소를 읽으면 stopPoint · meetingPoint의 lat · lng가 소수점 6자리까지 같다")
	void seededSpotIsLoadedWithStopAndMeetingPointEqualToSixDecimals() {
		PickupSpot loaded = loadSpotPort.loadSpot(seeded.defaultSpotId()).orElseThrow();

		assertThat(loaded.getName()).isEqualTo(PlaceSeed.DEFAULT_SPOT_NAME);
		assertThat(loaded.isDefault()).isTrue();
		assertThat(loaded.getStopPoint().lat()).isCloseTo(PlaceSeed.DEFAULT_STOP_LAT, within(SIX_DECIMALS));
		assertThat(loaded.getStopPoint().lng()).isCloseTo(PlaceSeed.DEFAULT_STOP_LNG, within(SIX_DECIMALS));
		assertThat(loaded.getMeetingPoint().lat()).isCloseTo(PlaceSeed.DEFAULT_MEETING_LAT, within(SIX_DECIMALS));
		assertThat(loaded.getMeetingPoint().lng()).isCloseTo(PlaceSeed.DEFAULT_MEETING_LNG, within(SIX_DECIMALS));
	}

	@Test
	@DisplayName("loadSpots는 academyId가 null이면 그 가족의 모든 학원 장소를 돌려주고 다른 가족 장소는 뺀다")
	void loadSpotsWithoutAcademyReturnsAllSpotsOfFamilyOnly() {
		long otherAcademyId = persistOtherAcademy();
		long otherAcademySpotId = persistSpot(familyId, otherAcademyId, "다른 학원 앞", true, guardianId);
		UserSeed.Result otherFamily = seedOtherFamily();
		long otherFamilySpotId = persistSpot(otherFamily.familyId(), academyId, "남의 가족 장소", true,
				otherFamily.guardianId());

		List<PickupSpot> spots = loadSpotPort.loadSpots(familyId, null);

		assertThat(spots).extracting(PickupSpot::getSpotId)
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId(), otherAcademySpotId)
				.doesNotContain(otherFamilySpotId);
		assertThat(spots).extracting(PickupSpot::getFamilyId).containsOnly(familyId);
	}

	@Test
	@DisplayName("loadSpots는 academyId를 주면 그 가족의 그 학원 장소만 돌려준다")
	void loadSpotsWithAcademyReturnsOnlySpotsOfThatAcademy() {
		long otherAcademyId = persistOtherAcademy();
		long otherAcademySpotId = persistSpot(familyId, otherAcademyId, "다른 학원 앞", true, guardianId);
		UserSeed.Result otherFamily = seedOtherFamily();
		long otherFamilySpotId = persistSpot(otherFamily.familyId(), academyId, "남의 가족 장소", true,
				otherFamily.guardianId());

		assertThat(loadSpotPort.loadSpots(familyId, academyId)).extracting(PickupSpot::getSpotId)
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId())
				.doesNotContain(otherAcademySpotId, otherFamilySpotId);
		assertThat(loadSpotPort.loadSpots(familyId, otherAcademyId)).extracting(PickupSpot::getSpotId)
				.containsExactly(otherAcademySpotId);
	}

	@Test
	@DisplayName("loadSpots는 장소가 없는 가족이면 빈 목록을 돌려준다")
	void loadSpotsReturnsEmptyListForFamilyWithoutSpots() {
		UserSeed.Result otherFamily = seedOtherFamily();

		assertThat(loadSpotPort.loadSpots(otherFamily.familyId(), null)).isEmpty();
		assertThat(loadSpotPort.loadSpots(otherFamily.familyId(), academyId)).isEmpty();
	}

	@Test
	@DisplayName("loadDefaultSpot은 가족 · 학원의 isDefault true 장소를 돌려주고 다른 가족 · 학원의 기본 장소는 돌려주지 않는다")
	void loadDefaultSpotReturnsDefaultSpotOfFamilyAndAcademy() {
		long otherAcademyId = persistOtherAcademy();
		long otherAcademyDefaultId = persistSpot(familyId, otherAcademyId, "다른 학원 기본", true, guardianId);
		UserSeed.Result otherFamily = seedOtherFamily();
		long otherFamilyDefaultId = persistSpot(otherFamily.familyId(), academyId, "남의 가족 기본", true,
				otherFamily.guardianId());

		PickupSpot found = loadSpotPort.loadDefaultSpot(familyId, academyId).orElseThrow();
		assertThat(found.getSpotId()).isEqualTo(seeded.defaultSpotId());
		assertThat(found.isDefault()).isTrue();
		assertThat(found.getName()).isEqualTo(PlaceSeed.DEFAULT_SPOT_NAME);

		assertThat(loadSpotPort.loadDefaultSpot(familyId, otherAcademyId)).map(PickupSpot::getSpotId)
				.contains(otherAcademyDefaultId);
		assertThat(loadSpotPort.loadDefaultSpot(otherFamily.familyId(), academyId)).map(PickupSpot::getSpotId)
				.contains(otherFamilyDefaultId);
	}

	@Test
	@DisplayName("loadDefaultSpot은 그 가족 · 학원에 isDefault true 장소가 없으면 빈 값을 돌려준다")
	void loadDefaultSpotIsEmptyWhenFamilyAcademyHasNoDefaultSpot() {
		long otherAcademyId = persistOtherAcademy();
		persistSpot(familyId, otherAcademyId, "기본 아닌 장소", false, guardianId);
		UserSeed.Result otherFamily = seedOtherFamily();

		Optional<PickupSpot> noDefault = loadSpotPort.loadDefaultSpot(familyId, otherAcademyId);
		Optional<PickupSpot> noSpots = loadSpotPort.loadDefaultSpot(otherFamily.familyId(), academyId);

		assertThat(noDefault).as("기본 아닌 장소만 있는 학원").isEmpty();
		assertThat(noSpots).as("장소가 없는 가족 (같은 학원에 다른 가족 기본 장소가 있어도)").isEmpty();
	}

	@Test
	@DisplayName("loadSpotsWithin은 near에서 radiusM 안에 정차 지점이 있는 장소만 돌려준다: 499m 안 · 501m 밖, 만남 지점만 안이면 빼고, 다른 가족 장소는 뺀다")
	void loadSpotsWithinReturnsOnlySpotsWhoseStopPointIsWithinRadius() {
		LatLng north300 = GeoOffsets.northOf(NEAR, 300);
		LatLng north499 = GeoOffsets.northOf(NEAR, 499);
		LatLng north501 = GeoOffsets.northOf(NEAR, 501);
		LatLng north1000 = GeoOffsets.northOf(NEAR, 1000);
		long insideBoundaryId = persistSpotAt(familyId, academyId, "499m 정차", north499, north499, guardianId);
		long outsideBoundaryId = persistSpotAt(familyId, academyId, "501m 정차", north501, north501, guardianId);
		long onlyMeetingInsideId = persistSpotAt(familyId, academyId, "만남만 안", north1000, NEAR, guardianId);
		long onlyStopInsideId = persistSpotAt(familyId, academyId, "정차만 안", north300, north1000, guardianId);
		UserSeed.Result otherFamily = seedOtherFamily();
		long otherFamilySpotId = persistSpotAt(otherFamily.familyId(), academyId, "남의 가족 장소", NEAR, NEAR,
				otherFamily.guardianId());

		List<PickupSpot> spots = loadSpotPort.loadSpotsWithin(familyId, academyId, NEAR, RADIUS_M);

		assertThat(spots).extracting(PickupSpot::getSpotId)
				.as("정차 지점이 500m 안인 내 가족 장소")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId(), insideBoundaryId,
						onlyStopInsideId)
				.doesNotContain(outsideBoundaryId, onlyMeetingInsideId, otherFamilySpotId);
		assertThat(spots).extracting(PickupSpot::getFamilyId).containsOnly(familyId);
		PickupSpot loadedDefault = spots.stream()
				.filter(spot -> spot.getSpotId().equals(seeded.defaultSpotId()))
				.findFirst()
				.orElseThrow();
		assertThat(loadedDefault.getName()).isEqualTo(PlaceSeed.DEFAULT_SPOT_NAME);
		assertThat(loadedDefault.isDefault()).isTrue();
		assertThat(loadedDefault.getStopPoint().lat()).isCloseTo(PlaceSeed.DEFAULT_STOP_LAT, within(SIX_DECIMALS));
		assertThat(loadedDefault.getStopPoint().lng()).isCloseTo(PlaceSeed.DEFAULT_STOP_LNG, within(SIX_DECIMALS));
	}

	@Test
	@DisplayName("loadSpotsWithin은 academyId가 null이면 반경 안의 내 가족 모든 학원 장소를, academyId를 주면 그 학원 장소만 돌려준다")
	void loadSpotsWithinFiltersByAcademyOnlyWhenAcademyIdIsGiven() {
		long otherAcademyId = persistOtherAcademy();
		long otherAcademySpotId = persistSpotAt(familyId, otherAcademyId, "다른 학원 반경 안", NEAR, NEAR, guardianId);
		long otherAcademyFarSpotId = persistSpotAt(familyId, otherAcademyId, "다른 학원 반경 밖",
				GeoOffsets.northOf(NEAR, 501), GeoOffsets.northOf(NEAR, 501), guardianId);

		assertThat(loadSpotPort.loadSpotsWithin(familyId, null, NEAR, RADIUS_M)).extracting(PickupSpot::getSpotId)
				.as("academyId null")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId(), otherAcademySpotId)
				.doesNotContain(otherAcademyFarSpotId);
		assertThat(loadSpotPort.loadSpotsWithin(familyId, academyId, NEAR, RADIUS_M)).extracting(PickupSpot::getSpotId)
				.as("시드 학원만")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId());
		assertThat(loadSpotPort.loadSpotsWithin(familyId, otherAcademyId, NEAR, RADIUS_M))
				.extracting(PickupSpot::getSpotId)
				.as("다른 학원만")
				.containsExactly(otherAcademySpotId);
	}

	@Test
	@DisplayName("loadSpotsWithin은 반경 안에 정차 지점이 없으면 빈 목록을 돌려준다")
	void loadSpotsWithinReturnsEmptyListWhenNoStopPointIsWithinRadius() {
		LatLng farAway = GeoOffsets.northOf(NEAR, 5000);

		assertThat(loadSpotPort.loadSpotsWithin(familyId, null, farAway, RADIUS_M)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("saveSpot으로 spotId가 있는 장소를 다시 저장하면 새 행을 만들지 않고 그 행의 이름 · 좌표 · isDefault가 바뀐다")
	void saveSpotWithExistingSpotIdUpdatesThatRowWithoutInsertingNewRow() {
		long spotId = seeded.defaultSpotId();
		Integer totalBefore = jdbcTemplate.queryForObject("select count(*) from pickup_spot", Integer.class);
		Integer familyBefore = jdbcTemplate.queryForObject("select count(*) from pickup_spot where family_id = ?",
				Integer.class, familyId);

		PickupSpot saved = saveSpotPort.saveSpot(PickupSpot.restore(spotId, familyId, academyId, "새 정문",
				PlaceSeed.DEFAULT_SPOT_MEMO, SAVED_STOP, SAVED_MEETING, false, guardianId));

		assertThat(saved.getSpotId()).as("돌려받은 spotId").isEqualTo(spotId);
		Integer totalAfter = jdbcTemplate.queryForObject("select count(*) from pickup_spot", Integer.class);
		Integer familyAfter = jdbcTemplate.queryForObject("select count(*) from pickup_spot where family_id = ?",
				Integer.class, familyId);
		assertThat(totalAfter).as("전체 행 수").isEqualTo(totalBefore);
		assertThat(familyAfter).as("시드 가족 행 수").isEqualTo(familyBefore).isEqualTo(2);
		assertThat(countSpotRows(spotId)).as("그 spotId 행 수").isEqualTo(1);

		Map<String, Object> row = jdbcTemplate.queryForMap("""
				select name, is_default,
				       st_y(stop_point::geometry) as stop_lat, st_x(stop_point::geometry) as stop_lng,
				       st_y(meeting_point::geometry) as meeting_lat, st_x(meeting_point::geometry) as meeting_lng
				from pickup_spot
				where spot_id = ?
				""", spotId);
		assertThat(row.get("name")).isEqualTo("새 정문");
		assertThat(row.get("is_default")).isEqualTo(false);
		assertThat((Double) row.get("stop_lat")).as("DB stop_point lat").isCloseTo(SAVED_STOP.lat(), within(SIX_DECIMALS));
		assertThat((Double) row.get("stop_lng")).as("DB stop_point lng").isCloseTo(SAVED_STOP.lng(), within(SIX_DECIMALS));
		assertThat((Double) row.get("meeting_lat")).as("DB meeting_point lat")
				.isCloseTo(SAVED_MEETING.lat(), within(SIX_DECIMALS));
		assertThat((Double) row.get("meeting_lng")).as("DB meeting_point lng")
				.isCloseTo(SAVED_MEETING.lng(), within(SIX_DECIMALS));
	}

	@Test
	@DisplayName("deleteSpot은 그 장소 행만 지우고 같은 가족 · 학원의 다른 장소는 남긴다")
	void deleteSpotRemovesOnlyThatSpotAndKeepsOtherSpotsOfSameFamilyAndAcademy() {
		long otherAcademyId = persistOtherAcademy();
		long otherAcademySpotId = persistSpot(familyId, otherAcademyId, "다른 학원 앞", true, guardianId);

		saveSpotPort.deleteSpot(seeded.otherSpotId());

		assertThat(loadSpotPort.loadSpot(seeded.otherSpotId())).as("지운 장소").isEmpty();
		assertThat(countSpotRows(seeded.otherSpotId())).as("지운 장소 행 수").isZero();
		assertThat(countSpotRows(seeded.defaultSpotId())).as("같은 가족 · 학원의 다른 장소 행 수").isEqualTo(1);
		assertThat(countSpotRows(otherAcademySpotId)).as("같은 가족 다른 학원 장소 행 수").isEqualTo(1);
		assertThat(loadSpotPort.loadSpots(familyId, academyId)).extracting(PickupSpot::getSpotId)
				.containsExactly(seeded.defaultSpotId());
		PickupSpot remaining = loadSpotPort.loadSpot(seeded.defaultSpotId()).orElseThrow();
		assertThat(remaining.getName()).isEqualTo(PlaceSeed.DEFAULT_SPOT_NAME);
		assertThat(remaining.isDefault()).isTrue();
	}

	@Test
	@DisplayName("deleteSpot은 없는 spotId면 예외 없이 아무 행도 지우지 않는다")
	void deleteSpotWithUnknownSpotIdDoesNothing() {
		long unknownSpotId = jdbcTemplate.queryForObject("select coalesce(max(spot_id), 0) + 1000 from pickup_spot",
				Long.class);
		Integer before = jdbcTemplate.queryForObject("select count(*) from pickup_spot where family_id = ?",
				Integer.class, familyId);

		saveSpotPort.deleteSpot(unknownSpotId);

		Integer after = jdbcTemplate.queryForObject("select count(*) from pickup_spot where family_id = ?",
				Integer.class, familyId);
		assertThat(after).as("시드 가족 장소 행 수").isEqualTo(before).isEqualTo(2);
	}

	@Test
	@DisplayName("saveSpot으로 기본 장소가 있는 가족 · 학원에 isDefault true 새 장소를 저장하면 DefaultSpotConflictException으로 거절하고 행을 만들지 않는다")
	void saveSpotRejectsNewSecondDefaultSpotOfSameFamilyAndAcademyWithDefaultSpotConflict() {
		assertThatThrownBy(() -> saveSpotPort.saveSpot(PickupSpot.create(familyId, academyId, "옆문 앞", null,
				SAVED_STOP, SAVED_MEETING, true, guardianId)))
				.isInstanceOf(DefaultSpotConflictException.class);

		assertThat(spotIdsOfFamilyAndAcademy()).as("시드 가족 · 학원 장소")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId());
		assertThat(defaultSpotIdsOfFamilyAndAcademy()).as("시드 가족 · 학원의 기본 장소")
				.containsExactly(seeded.defaultSpotId());
	}

	@Test
	@DisplayName("saveSpot으로 기본 장소가 있는 가족 · 학원의 다른 장소를 isDefault true로 다시 저장하면 DefaultSpotConflictException으로 거절하고 그 행은 바뀌지 않는다")
	void saveSpotRejectsPromotingAnotherSpotToSecondDefaultWithDefaultSpotConflict() {
		PickupSpot promoted = PickupSpot.restore(seeded.otherSpotId(), familyId, academyId, PlaceSeed.OTHER_SPOT_NAME,
				null, new LatLng(PlaceSeed.OTHER_STOP_LAT, PlaceSeed.OTHER_STOP_LNG),
				new LatLng(PlaceSeed.OTHER_MEETING_LAT, PlaceSeed.OTHER_MEETING_LNG), true, guardianId);

		assertThatThrownBy(() -> saveSpotPort.saveSpot(promoted))
				.isInstanceOf(DefaultSpotConflictException.class);

		assertThat(defaultSpotIdsOfFamilyAndAcademy()).as("시드 가족 · 학원의 기본 장소")
				.containsExactly(seeded.defaultSpotId());
		assertThat(jdbcTemplate.queryForObject("select is_default from pickup_spot where spot_id = ?", Boolean.class,
				seeded.otherSpotId())).as("다른 장소 is_default").isFalse();
	}

	@Test
	@DisplayName("loadSpots는 temporary true인 임시 장소를 빼고 돌려준다(academyId null · academyId 지정 모두). 임시 장소는 spotId로는 읽히고 temporary true다")
	void loadSpotsExcludesTemporarySpots() {
		long temporarySpotId = saveTemporarySpot(academyId, "여기로 부르기", NEAR);

		PickupSpot loadedTemporary = loadSpotPort.loadSpot(temporarySpotId).orElseThrow();
		assertThat(loadedTemporary.isTemporary()).as("저장한 임시 장소의 temporary").isTrue();
		assertThat(loadedTemporary.isDefault()).as("저장한 임시 장소의 isDefault").isFalse();

		assertThat(loadSpotPort.loadSpots(familyId, null)).extracting(PickupSpot::getSpotId)
				.as("academyId null")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId())
				.doesNotContain(temporarySpotId);
		assertThat(loadSpotPort.loadSpots(familyId, academyId)).extracting(PickupSpot::getSpotId)
				.as("시드 학원")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId())
				.doesNotContain(temporarySpotId);
	}

	@Test
	@DisplayName("loadSpots는 임시 장소만 있는 가족 · 학원이면 빈 목록을 돌려준다(첫 장소 기본 지정 판정이 임시 장소를 세지 않게)")
	void loadSpotsReturnsEmptyListWhenAcademyHasOnlyTemporarySpots() {
		long otherAcademyId = persistOtherAcademy();
		long temporarySpotId = saveTemporarySpot(otherAcademyId, "추천 장소", OTHER_ACADEMY_LOCATION);

		assertThat(loadSpotPort.loadSpots(familyId, otherAcademyId)).extracting(PickupSpot::getSpotId)
				.doesNotContain(temporarySpotId)
				.isEmpty();
	}

	@Test
	@DisplayName("loadSpotsWithin은 반경 안에 정차 지점이 있어도 temporary true인 임시 장소는 빼고 돌려준다")
	void loadSpotsWithinExcludesTemporarySpots() {
		long temporarySpotId = saveTemporarySpot(academyId, "여기로 부르기", NEAR);

		assertThat(loadSpotPort.loadSpotsWithin(familyId, academyId, NEAR, RADIUS_M)).extracting(PickupSpot::getSpotId)
				.as("시드 학원")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId())
				.doesNotContain(temporarySpotId);
		assertThat(loadSpotPort.loadSpotsWithin(familyId, null, NEAR, RADIUS_M)).extracting(PickupSpot::getSpotId)
				.as("academyId null")
				.containsExactlyInAnyOrder(seeded.defaultSpotId(), seeded.otherSpotId())
				.doesNotContain(temporarySpotId);
	}

	@Test
	@DisplayName("loadDefaultSpot은 is_default true인 행이라도 temporary true인 임시 장소면 기본 장소로 돌려주지 않는다")
	void loadDefaultSpotExcludesTemporarySpots() {
		long otherAcademyId = persistOtherAcademy();
		long temporarySpotId = saveTemporarySpot(otherAcademyId, "추천 장소", OTHER_ACADEMY_LOCATION);
		jdbcTemplate.update("update pickup_spot set is_default = true where spot_id = ?", temporarySpotId);

		assertThat(loadSpotPort.loadDefaultSpot(familyId, otherAcademyId)).as("임시 장소만 있는 학원").isEmpty();
		assertThat(loadSpotPort.loadDefaultSpot(familyId, academyId)).map(PickupSpot::getSpotId)
				.as("시드 학원의 기본 장소")
				.contains(seeded.defaultSpotId());
	}

	private long saveTemporarySpot(long spotAcademyId, String name, LatLng point) {
		return saveSpotPort.saveSpot(PickupSpot.createTemporary(familyId, spotAcademyId, name, point, point))
				.getSpotId();
	}

	private List<Long> spotIdsOfFamilyAndAcademy() {
		return jdbcTemplate.queryForList("select spot_id from pickup_spot where family_id = ? and academy_id = ?",
				Long.class, familyId, academyId);
	}

	private List<Long> defaultSpotIdsOfFamilyAndAcademy() {
		return jdbcTemplate.queryForList(
				"select spot_id from pickup_spot where family_id = ? and academy_id = ? and is_default",
				Long.class, familyId, academyId);
	}

	private int countSpotRows(long spotId) {
		return jdbcTemplate.queryForObject("select count(*) from pickup_spot where spot_id = ?", Integer.class, spotId);
	}

	private UserSeed.Result seedOtherFamily() {
		UserSeed.Result family = userSeed.seed();
		otherFamilies.add(family);
		return family;
	}

	private long persistOtherAcademy() {
		long id = transactionTemplate.execute(status -> {
			AcademyEntity academy = new AcademyEntity(null, "kakao:place-test-" + UUID.randomUUID(), "다른학원",
					"서울 강남구 강남대로 396",
					toPoint(OTHER_ACADEMY_LOCATION), toPoint(OTHER_ACADEMY_LOCATION), null);
			entityManager.persist(academy);
			entityManager.flush();
			return academy.getAcademyId();
		});
		otherAcademyIds.add(id);
		return id;
	}

	private long persistSpotAt(long spotFamilyId, long spotAcademyId, String name, LatLng stop, LatLng meeting,
			long createdBy) {
		return persistEntity(new PickupSpotEntity(null, spotFamilyId, spotAcademyId, name, null, toPoint(stop),
				toPoint(meeting), false, createdBy));
	}

	private long persistSpot(long spotFamilyId, long spotAcademyId, String name, boolean isDefault, long createdBy) {
		return persistEntity(new PickupSpotEntity(null, spotFamilyId, spotAcademyId, name, null,
				toPoint(OTHER_ACADEMY_LOCATION), toPoint(OTHER_ACADEMY_LOCATION), isDefault, createdBy));
	}

	private long persistEntity(PickupSpotEntity spot) {
		return transactionTemplate.execute(status -> {
			entityManager.persist(spot);
			entityManager.flush();
			return spot.getSpotId();
		});
	}

	private static Point toPoint(LatLng latLng) {
		return GeoPoints.toPoint(latLng.lat(), latLng.lng());
	}

}

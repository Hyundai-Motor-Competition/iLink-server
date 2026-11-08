package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.fixture.GeoOffsets;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.AddPickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.AddSpotCommand;
import com.example.pickupservice.place.application.port.in.SpotCreated;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 DB(기본 장소 부분 유니크 인덱스 포함)에서 기본 장소 바꾸기가 인덱스와 부딪히지 않는지 본다. */
@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class})
class AddPickupSpotServiceIntegrationTest extends IntegrationTestSupport {

	private static final LatLng ACADEMY = new LatLng(ScheduleSeed.ACADEMY_LAT, ScheduleSeed.ACADEMY_LNG);

	private final AddPickupSpotUseCase addPickupSpotUseCase;
	private final PlaceSeed placeSeed;
	private final JdbcTemplate jdbcTemplate;

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
		placeSeed.clear(seeded);
	}

	@Test
	@DisplayName("기본 장소가 있는 학원에 isDefault true로 장소를 추가하면 유니크 인덱스와 부딪히지 않고 새 장소만 기본이 되고 이전 기본은 false가 된다")
	void addSpotWithIsDefaultTrueSwitchesDefaultWithoutHittingUniqueIndex() {
		assertThat(DefaultSpotIndexes.of(jdbcTemplate)).as("전제: 기본 장소 부분 유니크 인덱스").hasSize(1);
		LatLng point = GeoOffsets.northOf(ACADEMY, 100);

		SpotCreated created = addPickupSpotUseCase.addSpot(new AddSpotCommand(familyId, guardianId, academyId,
				"옆문 앞", point, point, null, true));

		assertThat(created.isDefault()).as("새 장소 isDefault").isTrue();
		assertThat(defaultSpotIds()).as("가족 · 학원의 기본 장소").containsExactly(created.spotId());
		assertThat(isDefault(seeded.defaultSpotId())).as("이전 기본 장소 is_default").isFalse();
		assertThat(isDefault(seeded.otherSpotId())).as("다른 장소 is_default").isFalse();
	}

	private List<Long> defaultSpotIds() {
		return jdbcTemplate.queryForList(
				"select spot_id from pickup_spot where family_id = ? and academy_id = ? and is_default",
				Long.class, familyId, academyId);
	}

	private Boolean isDefault(long spotId) {
		return jdbcTemplate.queryForObject("select is_default from pickup_spot where spot_id = ?", Boolean.class,
				spotId);
	}

}

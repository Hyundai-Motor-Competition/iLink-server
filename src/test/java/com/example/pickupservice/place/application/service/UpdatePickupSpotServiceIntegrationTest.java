package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.place.application.port.in.SpotView;
import com.example.pickupservice.place.application.port.in.UpdatePickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.UpdateSpotCommand;
import com.example.pickupservice.place.fixture.DefaultSpotIndexes;
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

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class})
class UpdatePickupSpotServiceIntegrationTest extends IntegrationTestSupport {

	private final UpdatePickupSpotUseCase updatePickupSpotUseCase;
	private final PlaceSeed placeSeed;
	private final JdbcTemplate jdbcTemplate;

	private PlaceSeed.Result seeded;
	private long familyId;
	private long academyId;

	@BeforeEach
	void seed() {
		seeded = placeSeed.seed();
		familyId = seeded.schedule().user().familyId();
		academyId = seeded.schedule().academyId();
	}

	@AfterEach
	void clear() {
		placeSeed.clear(seeded);
	}

	@Test
	@DisplayName("시드의 다른 장소를 PATCH isDefault true로 바꾸면 유니크 인덱스와 부딪히지 않고 그 장소만 기본이 되고 이전 기본은 false가 된다")
	void updateSpotWithIsDefaultTrueSwitchesDefaultWithoutHittingUniqueIndex() {
		assertThat(DefaultSpotIndexes.partialUniqueOnFamilyAcademy(jdbcTemplate)).as("전제: 기본 장소 부분 유니크 인덱스").hasSize(1);

		SpotView view = updatePickupSpotUseCase.updateSpot(
				new UpdateSpotCommand(familyId, seeded.otherSpotId(), null, null, null, null, true));

		assertThat(view.spotId()).isEqualTo(seeded.otherSpotId());
		assertThat(view.isDefault()).as("고친 장소 isDefault").isTrue();
		assertThat(defaultSpotIds()).as("가족 · 학원의 기본 장소").containsExactly(seeded.otherSpotId());
		assertThat(jdbcTemplate.queryForObject("select is_default from pickup_spot where spot_id = ?", Boolean.class,
				seeded.defaultSpotId())).as("이전 기본 장소 is_default").isFalse();
	}

	private List<Long> defaultSpotIds() {
		return jdbcTemplate.queryForList(
				"select spot_id from pickup_spot where family_id = ? and academy_id = ? and is_default",
				Long.class, familyId, academyId);
	}

}

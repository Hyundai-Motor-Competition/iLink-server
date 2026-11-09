package com.example.pickupservice.place.fixture;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class})
class PlaceSeedTest extends IntegrationTestSupport {

	private static final double COORDINATE_TOLERANCE = 0.000001;

	private final PlaceSeed placeSeed;
	private final JdbcTemplate jdbcTemplate;

	private PlaceSeed.Result seeded;

	@AfterEach
	void cleanUp() {
		if (seeded != null) {
			placeSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("테스트 시드로 해솔영어학원의 기본 장소 정문 앞과 다른 장소 1개를 만든다")
	void seedCreatesDefaultFrontGateSpotAndOneOtherSpotForHaesolAcademy() {
		seeded = placeSeed.seed();
		long familyId = seeded.schedule().user().familyId();
		long academyId = seeded.schedule().academyId();

		String academyName = jdbcTemplate.queryForObject(
				"select name from academy where academy_id = ?", String.class, academyId);
		assertThat(academyName).isEqualTo("해솔영어학원");

		List<Map<String, Object>> spots = jdbcTemplate.queryForList("""
				select spot_id, family_id, academy_id, name, memo, is_default, created_by,
				       st_y(stop_point::geometry) as stop_lat, st_x(stop_point::geometry) as stop_lng,
				       st_y(meeting_point::geometry) as meeting_lat, st_x(meeting_point::geometry) as meeting_lng,
				       st_distance(stop_point, (select location from academy where academy_id = ?)) as distance_m
				from pickup_spot
				where family_id = ?
				order by spot_id
				""", academyId, familyId);

		assertThat(spots).hasSize(2);
		assertThat(spots).allSatisfy(spot -> {
			assertThat(((Number) spot.get("academy_id")).longValue()).isEqualTo(academyId);
			assertThat(((Number) spot.get("created_by")).longValue()).isEqualTo(seeded.schedule().user().guardianId());
			assertThat(((Number) spot.get("distance_m")).doubleValue()).as("학원 500m 안").isLessThanOrEqualTo(500.0);
		});
		assertThat(spots).filteredOn(spot -> Boolean.TRUE.equals(spot.get("is_default"))).singleElement()
				.satisfies(spot -> {
					assertThat(((Number) spot.get("spot_id")).longValue()).isEqualTo(seeded.defaultSpotId());
					assertThat(spot.get("name")).isEqualTo("정문 앞");
					assertThat(spot.get("memo")).isEqualTo("편의점 앞");
					assertThat((Double) spot.get("stop_lat")).isCloseTo(37.501234, within(COORDINATE_TOLERANCE));
					assertThat((Double) spot.get("stop_lng")).isCloseTo(127.039876, within(COORDINATE_TOLERANCE));
					assertThat((Double) spot.get("meeting_lat")).isCloseTo(37.501301, within(COORDINATE_TOLERANCE));
					assertThat((Double) spot.get("meeting_lng")).isCloseTo(127.039901, within(COORDINATE_TOLERANCE));
				});
		assertThat(spots).filteredOn(spot -> Boolean.FALSE.equals(spot.get("is_default"))).singleElement()
				.satisfies(spot -> {
					assertThat(((Number) spot.get("spot_id")).longValue()).isEqualTo(seeded.otherSpotId());
					assertThat(spot.get("name")).isEqualTo(PlaceSeed.OTHER_SPOT_NAME).isNotEqualTo("정문 앞");
					assertThat((Double) spot.get("stop_lat"))
							.isCloseTo(PlaceSeed.OTHER_STOP_LAT, within(COORDINATE_TOLERANCE));
					assertThat((Double) spot.get("stop_lng"))
							.isCloseTo(PlaceSeed.OTHER_STOP_LNG, within(COORDINATE_TOLERANCE));
					assertThat((Double) spot.get("meeting_lat"))
							.isCloseTo(PlaceSeed.OTHER_MEETING_LAT, within(COORDINATE_TOLERANCE));
					assertThat((Double) spot.get("meeting_lng"))
							.isCloseTo(PlaceSeed.OTHER_MEETING_LNG, within(COORDINATE_TOLERANCE));
				});
	}

}

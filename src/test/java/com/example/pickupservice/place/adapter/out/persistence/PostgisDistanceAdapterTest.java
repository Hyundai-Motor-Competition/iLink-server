package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.fixture.GeoOffsets;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.DistancePort;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
class PostgisDistanceAdapterTest extends IntegrationTestSupport {

	private static final int SPOT_LIMIT_METERS = 500;
	private static final LatLng ACADEMY = new LatLng(ScheduleSeed.ACADEMY_LAT, ScheduleSeed.ACADEMY_LNG);

	private final DistancePort distancePort;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("isWithinMeters는 학원에서 499m인 좌표에 true, 501m인 좌표에 false를 돌려준다")
	void isWithinMetersIsTrueAt499AndFalseAt501FromAcademy() {
		LatLng at499 = GeoOffsets.northOf(ACADEMY, 499);
		LatLng at501 = GeoOffsets.northOf(ACADEMY, 501);
		assertThat(geographyDistance(ACADEMY, at499)).as("테스트 좌표 확인: 학원 → 499m 좌표").isCloseTo(499.0, within(0.05));
		assertThat(geographyDistance(ACADEMY, at501)).as("테스트 좌표 확인: 학원 → 501m 좌표").isCloseTo(501.0, within(0.05));

		assertThat(distancePort.isWithinMeters(ACADEMY, at499, SPOT_LIMIT_METERS)).as("499m").isTrue();
		assertThat(distancePort.isWithinMeters(ACADEMY, at501, SPOT_LIMIT_METERS)).as("501m").isFalse();
	}

	private double geographyDistance(LatLng from, LatLng to) {
		return jdbcTemplate.queryForObject("""
				select st_distance(st_setsrid(st_makepoint(?, ?), 4326)::geography,
				                   st_setsrid(st_makepoint(?, ?), 4326)::geography)
				""", Double.class, from.lng(), from.lat(), to.lng(), to.lat());
	}
}

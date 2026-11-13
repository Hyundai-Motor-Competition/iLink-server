package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BlockDetourDistanceTest {

	private static final double SEOUL_LAT = 37.5;
	private static final double SEOUL_LNG = 127.0;

	private static final double NORTH_300M_LAT_DELTA_AT_SEOUL = 0.0026980;
	private static final double EAST_400M_LNG_DELTA_AT_SEOUL = 0.0045343;

	@Test
	@DisplayName("블록 우회 거리 규칙은 북쪽 300m · 동쪽 400m 떨어진 두 점 사이를 직선 500m가 아니라 약 700m(±2m)로 돌려준다")
	void northThreeHundredAndEastFourHundredIsAboutSevenHundredMeters() {
		LatLng from = new LatLng(SEOUL_LAT, SEOUL_LNG);
		LatLng to = new LatLng(SEOUL_LAT + NORTH_300M_LAT_DELTA_AT_SEOUL, SEOUL_LNG + EAST_400M_LNG_DELTA_AT_SEOUL);

		double meters = BlockDetourDistance.meters(from, to);

		assertThat(meters).isCloseTo(700.0, within(2.0));
		assertThat(meters).isGreaterThan(StraightDistance.meters(from, to) + 100.0);
	}

	@Test
	@DisplayName("블록 우회 거리 규칙은 같은 좌표 사이 거리를 0m로 돌려준다")
	void samePointIsZeroMeters() {
		LatLng point = new LatLng(SEOUL_LAT, SEOUL_LNG);

		assertThat(BlockDetourDistance.meters(point, point)).isCloseTo(0.0, within(1e-9));
	}

	@Test
	@DisplayName("블록 우회 거리 규칙은 남북으로만 떨어진 두 점이면 직선거리와 같은 값을 돌려준다")
	void northSouthOnlyEqualsStraightDistance() {
		LatLng from = new LatLng(SEOUL_LAT, SEOUL_LNG);
		LatLng to = new LatLng(SEOUL_LAT + NORTH_300M_LAT_DELTA_AT_SEOUL, SEOUL_LNG);

		assertThat(BlockDetourDistance.meters(from, to))
				.isCloseTo(StraightDistance.meters(from, to), within(0.5))
				.isCloseTo(300.0, within(1.0));
	}

	@Test
	@DisplayName("블록 우회 거리 규칙은 동서로만 떨어진 두 점이면 직선거리와 같은 값을 돌려준다")
	void eastWestOnlyEqualsStraightDistance() {
		LatLng from = new LatLng(SEOUL_LAT, SEOUL_LNG);
		LatLng to = new LatLng(SEOUL_LAT, SEOUL_LNG + EAST_400M_LNG_DELTA_AT_SEOUL);

		assertThat(BlockDetourDistance.meters(from, to))
				.isCloseTo(StraightDistance.meters(from, to), within(0.5))
				.isCloseTo(400.0, within(1.0));
	}

	@Test
	@DisplayName("블록 우회 거리 규칙은 from과 to를 바꿔도 같은 거리를 돌려준다")
	void distanceIsSameWhenDirectionIsReversed() {
		LatLng from = new LatLng(SEOUL_LAT, SEOUL_LNG);
		LatLng to = new LatLng(SEOUL_LAT + NORTH_300M_LAT_DELTA_AT_SEOUL, SEOUL_LNG - EAST_400M_LNG_DELTA_AT_SEOUL);

		assertThat(BlockDetourDistance.meters(to, from))
				.isCloseTo(BlockDetourDistance.meters(from, to), within(1e-6))
				.isCloseTo(700.0, within(2.0));
	}
}

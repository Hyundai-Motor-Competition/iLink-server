package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StraightDistanceTest {

	private static final double SEOUL_LAT = 37.5;
	private static final double SEOUL_LNG = 127.0;

	@Test
	@DisplayName("직선거리 규칙은 같은 좌표 사이 거리를 0m로 돌려준다")
	void samePointIsZeroMeters() {
		LatLng point = new LatLng(SEOUL_LAT, SEOUL_LNG);

		assertThat(StraightDistance.meters(point, point)).isCloseTo(0.0, within(1e-9));
	}

	@Test
	@DisplayName("직선거리 규칙은 위도 0.001도 차이를 약 111m(±1m)로 돌려준다")
	void latitudeDifferenceOfThousandthDegreeIsAboutOneHundredElevenMeters() {
		LatLng from = new LatLng(SEOUL_LAT, SEOUL_LNG);
		LatLng to = new LatLng(SEOUL_LAT + 0.001, SEOUL_LNG);

		assertThat(StraightDistance.meters(from, to)).isCloseTo(111.0, within(1.0));
	}

	@Test
	@DisplayName("직선거리 규칙은 서울 위도 37.5도에서 경도 0.001도 차이를 약 88m(±1m)로 돌려준다")
	void longitudeDifferenceOfThousandthDegreeAtSeoulLatitudeIsAboutEightyEightMeters() {
		LatLng from = new LatLng(SEOUL_LAT, SEOUL_LNG);
		LatLng to = new LatLng(SEOUL_LAT, SEOUL_LNG + 0.001);

		assertThat(StraightDistance.meters(from, to)).isCloseTo(88.0, within(1.0));
	}

	@Test
	@DisplayName("직선거리 규칙은 from과 to를 바꿔도 같은 거리를 돌려준다")
	void distanceIsSameWhenDirectionIsReversed() {
		LatLng from = new LatLng(SEOUL_LAT, SEOUL_LNG);
		LatLng to = new LatLng(SEOUL_LAT + 0.002, SEOUL_LNG + 0.003);

		assertThat(StraightDistance.meters(to, from))
				.isCloseTo(StraightDistance.meters(from, to), within(1e-9));
	}
}

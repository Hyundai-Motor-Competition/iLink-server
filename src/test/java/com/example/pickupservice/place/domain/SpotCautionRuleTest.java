package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotCautionRuleTest {

	private static SpotCandidate candidate(SpotKind kind, LineKind lineKind, boolean cctv) {
		return new SpotCandidate(kind, "장소", new LatLng(37.5, 127.0), 100, 91, lineKind, cctv);
	}

	@Test
	@DisplayName("주의 표시 규칙은 실선 없는 CURB_STOP에 LINE_DATA_2021만 붙인다")
	void curbStopGetsLineData2021() {
		List<SpotCaution> result = SpotCautionRule.cautionsOf(candidate(SpotKind.CURB_STOP, LineKind.NONE, false));

		assertThat(result).containsExactly(SpotCaution.LINE_DATA_2021);
	}

	@Test
	@DisplayName("주의 표시 규칙은 DOTTED_YELLOW 길가에 LINE_DATA_2021 다음으로 STOP_5MIN_ONLY를 붙인다")
	void dottedYellowGetsStop5MinOnly() {
		List<SpotCaution> result = SpotCautionRule.cautionsOf(candidate(SpotKind.CURB_STOP, LineKind.DOTTED_YELLOW, false));

		assertThat(result).containsExactly(SpotCaution.LINE_DATA_2021, SpotCaution.STOP_5MIN_ONLY);
	}

	@Test
	@DisplayName("주의 표시 규칙은 단속 카메라 근처 길가에 ENFORCEMENT_CAMERA_NEAR를 붙인다")
	void nearEnforcementCctvGetsCameraCaution() {
		List<SpotCaution> result = SpotCautionRule.cautionsOf(candidate(SpotKind.CURB_STOP, LineKind.NONE, true));

		assertThat(result).containsExactly(SpotCaution.LINE_DATA_2021, SpotCaution.ENFORCEMENT_CAMERA_NEAR);
	}

	@Test
	@DisplayName("주의 표시 규칙은 세 조건이 모두 맞으면 LINE_DATA_2021, STOP_5MIN_ONLY, ENFORCEMENT_CAMERA_NEAR 순서로 붙인다")
	void allCautionsInOrder() {
		List<SpotCaution> result = SpotCautionRule.cautionsOf(candidate(SpotKind.CURB_STOP, LineKind.DOTTED_YELLOW, true));

		assertThat(result).containsExactly(
				SpotCaution.LINE_DATA_2021, SpotCaution.STOP_5MIN_ONLY, SpotCaution.ENFORCEMENT_CAMERA_NEAR);
	}

	@Test
	@DisplayName("주의 표시 규칙은 PUBLIC_PARKING에는 아무것도 붙이지 않는다")
	void publicParkingGetsNothing() {
		List<SpotCaution> result = SpotCautionRule.cautionsOf(candidate(SpotKind.PUBLIC_PARKING, null, false));

		assertThat(result).isEmpty();
	}
}

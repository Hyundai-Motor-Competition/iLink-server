package com.example.pickupservice.place.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalkTimeRuleTest {

	@Test
	@DisplayName("걷는 시간 규칙은 직선거리 100m를 walkMeters 130 · walkSeconds 119로 돌려준다")
	void convertsHundredMeters() {
		WalkTime result = WalkTimeRule.fromStraightMeters(100);

		assertThat(result.walkMeters()).isEqualTo(130);
		assertThat(result.walkSeconds()).isEqualTo(119);
	}

	@Test
	@DisplayName("걷는 시간 규칙은 직선거리 0m를 walkMeters 0 · walkSeconds 0으로 돌려준다")
	void convertsZeroMeters() {
		WalkTime result = WalkTimeRule.fromStraightMeters(0);

		assertThat(result.walkMeters()).isZero();
		assertThat(result.walkSeconds()).isZero();
	}

	@Test
	@DisplayName("걷는 시간 규칙은 walkMeters를 반올림한다: 0.4m는 1, 0.3m는 0")
	void roundsWalkMetersToNearest() {
		assertThat(WalkTimeRule.fromStraightMeters(0.4).walkMeters()).isEqualTo(1);
		assertThat(WalkTimeRule.fromStraightMeters(0.3).walkMeters()).isZero();
		assertThat(WalkTimeRule.fromStraightMeters(11).walkMeters()).isEqualTo(14);
	}

	@Test
	@DisplayName("걷는 시간 규칙은 walkSeconds를 올림한다: walkMeters 1은 1초, 14는 13초")
	void roundsWalkSecondsUp() {
		assertThat(WalkTimeRule.fromStraightMeters(0.4).walkSeconds()).isEqualTo(1);
		assertThat(WalkTimeRule.fromStraightMeters(11).walkSeconds()).isEqualTo(13);
	}

	@Test
	@DisplayName("걷는 시간 규칙은 walkMeters가 1.1로 딱 나누어떨어지면 올림하지 않는다: 직선거리 110m는 143m · 130초")
	void doesNotRoundUpWhenWalkSecondsIsExact() {
		WalkTime result = WalkTimeRule.fromStraightMeters(110);

		assertThat(result.walkMeters()).isEqualTo(143);
		assertThat(result.walkSeconds()).isEqualTo(130);
	}

	@Test
	@DisplayName("걷는 시간 규칙은 직선거리 450m를 walkMeters 585 · walkSeconds 532로 돌려준다")
	void convertsCurbStopRadiusEdge() {
		WalkTime result = WalkTimeRule.fromStraightMeters(450);

		assertThat(result.walkMeters()).isEqualTo(585);
		assertThat(result.walkSeconds()).isEqualTo(532);
	}
}

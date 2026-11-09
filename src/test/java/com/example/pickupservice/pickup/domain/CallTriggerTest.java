package com.example.pickupservice.pickup.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallTriggerTest {

	private static final CallTrigger TRIGGER = new CallTrigger(180, 700);

	@Test
	@DisplayName("CallTrigger는 navEtaSeconds 180 · remainingMeters 2000이면 ETA_3MIN을 돌려준다")
	void evaluateReturnsEta3MinWhenEtaIsAtThresholdAndDistanceIsFar() {
		assertThat(TRIGGER.evaluate(180, 2000)).contains(CallTriggerType.ETA_3MIN);
	}

	@Test
	@DisplayName("CallTrigger는 navEtaSeconds 600 · remainingMeters 700이면 DIST_700M을 돌려준다")
	void evaluateReturnsDist700mWhenDistanceIsAtThresholdAndEtaIsLong() {
		assertThat(TRIGGER.evaluate(600, 700)).contains(CallTriggerType.DIST_700M);
	}

	@Test
	@DisplayName("CallTrigger는 navEtaSeconds 181 · remainingMeters 701이면 트리거 없음을 돌려준다")
	void evaluateReturnsEmptyWhenEtaAndDistanceAreJustOverThresholds() {
		assertThat(TRIGGER.evaluate(181, 701)).isEmpty();
	}

	@Test
	@DisplayName("CallTrigger는 ETA와 거리 기준이 둘 다 맞으면 ETA_3MIN을 돌려준다")
	void evaluateReturnsEta3MinWhenBothEtaAndDistanceMatch() {
		assertThat(TRIGGER.evaluate(180, 700)).contains(CallTriggerType.ETA_3MIN);
		assertThat(TRIGGER.evaluate(0, 0)).contains(CallTriggerType.ETA_3MIN);
	}

	@Test
	@DisplayName("CallTrigger는 생성할 때 받은 기준값으로 판정한다")
	void evaluateUsesThresholdsGivenAtConstruction() {
		CallTrigger stricter = new CallTrigger(120, 500);

		assertThat(stricter.evaluate(180, 700)).isEmpty();
		assertThat(stricter.evaluate(120, 2000)).contains(CallTriggerType.ETA_3MIN);
		assertThat(stricter.evaluate(600, 500)).contains(CallTriggerType.DIST_700M);
	}
}

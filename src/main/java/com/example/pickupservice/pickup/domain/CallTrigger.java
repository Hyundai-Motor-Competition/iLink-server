package com.example.pickupservice.pickup.domain;

import java.util.Optional;

public record CallTrigger(int etaThresholdSeconds, int distanceThresholdMeters) {

	public Optional<CallTriggerType> evaluate(int navEtaSeconds, int remainingMeters) {
		if (navEtaSeconds <= etaThresholdSeconds) {
			return Optional.of(CallTriggerType.ETA_3MIN);
		}
		if (remainingMeters <= distanceThresholdMeters) {
			return Optional.of(CallTriggerType.DIST_700M);
		}
		return Optional.empty();
	}
}

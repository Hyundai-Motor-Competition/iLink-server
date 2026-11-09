package com.example.pickupservice.pickup.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class HelpRequestRule {

	private HelpRequestRule() {
	}

	public static HelpRequestDecision decide(Optional<Instant> lastRequestedAt, Instant now, Duration interval) {
		if (lastRequestedAt.isPresent()) {
			Instant nextAllowedAt = lastRequestedAt.get().plus(interval);
			if (now.isBefore(nextAllowedAt)) {
				return new HelpRequestDecision(false, nextAllowedAt);
			}
		}
		return new HelpRequestDecision(true, now.plus(interval));
	}
}

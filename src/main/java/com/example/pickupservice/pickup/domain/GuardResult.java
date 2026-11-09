package com.example.pickupservice.pickup.domain;

import java.util.List;

public record GuardResult(boolean met, List<GuardId> unmet) {

	public static GuardResult of(List<GuardId> unmet) {
		return new GuardResult(unmet.isEmpty(), List.copyOf(unmet));
	}
}

package com.example.pickupservice.place.domain;

import java.util.List;

public final class SpotDeletionRule {

	private SpotDeletionRule() {
	}

	public static boolean canDelete(PickupSpot target, List<PickupSpot> existingSpots) {
		if (!target.isDefault()) {
			return true;
		}
		return existingSpots.stream().noneMatch(target::isSiblingOf);
	}
}

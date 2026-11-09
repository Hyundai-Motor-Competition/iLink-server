package com.example.pickupservice.place.domain;

import java.util.List;

public final class DefaultSpotRule {

	private DefaultSpotRule() {
	}

	public static PickupSpot applyFirstSpotDefault(PickupSpot candidate, List<PickupSpot> existingSpots) {
		boolean hasSibling = existingSpots.stream().anyMatch(candidate::isSiblingOf);
		return hasSibling ? candidate : candidate.withDefault(true);
	}

	public static List<PickupSpot> demotePreviousDefaults(PickupSpot newDefault, List<PickupSpot> existingSpots) {
		if (!newDefault.isDefault()) {
			return List.of();
		}
		return existingSpots.stream()
				.filter(newDefault::isSiblingOf)
				.filter(PickupSpot::isDefault)
				.map(spot -> spot.withDefault(false))
				.toList();
	}
}

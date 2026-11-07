package com.example.pickupservice.place.application.port.in;

/** schedule의 SpotSummary와 같은 모양. */
public record SpotSummary(
		long spotId,
		long academyId,
		String name,
		boolean isDefault
) {
}

package com.example.pickupservice.place.application.port.in;

public record SpotSummary(
		long spotId,
		long academyId,
		String name,
		boolean isDefault
) {
}

package com.example.pickupservice.schedule.application.port.out;

public record SpotSummary(
		long spotId,
		long academyId,
		String name,
		boolean isDefault
) {
}

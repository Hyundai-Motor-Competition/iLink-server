package com.example.pickupservice.schedule.application.port.in;

public record SetupSpot(
		long spotId,
		String name,
		boolean isDefault
) {
}

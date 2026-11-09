package com.example.pickupservice.pickup.application.port.in;

public record SessionSpotView(
		long spotId,
		String name,
		int spotVersion,
		boolean locked
) {
}

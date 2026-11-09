package com.example.pickupservice.place.application.port.in;

public record SpotCreated(
		long spotId,
		boolean isDefault
) {
}

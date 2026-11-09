package com.example.pickupservice.pickup.application.port.in;

public record StartPickupResult(
		boolean created,
		SessionView session
) {
}

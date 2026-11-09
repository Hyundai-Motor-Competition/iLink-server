package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record SessionIncidentView(
		String type,
		long childUserId,
		Instant openedAt
) {
}

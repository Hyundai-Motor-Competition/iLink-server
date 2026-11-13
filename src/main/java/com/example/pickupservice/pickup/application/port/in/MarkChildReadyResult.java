package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record MarkChildReadyResult(
		long childUserId,
		Instant readyAt
) {
}

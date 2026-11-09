package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record CallProgressView(
		Instant acceptedAt,
		Instant sentAt,
		Instant deliveredAt,
		Instant acknowledgedAt
) {
}

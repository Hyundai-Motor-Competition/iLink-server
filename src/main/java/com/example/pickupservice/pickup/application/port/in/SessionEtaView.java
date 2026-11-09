package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record SessionEtaView(
		Instant stopEtaAt,
		Integer remainingSeconds,
		Instant updatedAt,
		boolean stale
) {
}

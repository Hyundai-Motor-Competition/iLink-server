package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record CorrectionView(
		String status,
		Instant correctedStopAt
) {
}

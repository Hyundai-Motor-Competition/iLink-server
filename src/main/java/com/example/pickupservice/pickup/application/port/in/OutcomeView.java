package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record OutcomeView(
		long sessionId,
		Instant completedAt,
		String spotName,
		String boardingMethod,
		Instant predictedStopAt,
		Instant actualStopAt,
		Long diffSeconds,
		CorrectionView correction
) {
}

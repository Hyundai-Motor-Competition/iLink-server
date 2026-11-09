package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record SessionBoardingView(
		Instant detectedAt,
		Instant cancelableUntil,
		String door
) {
}

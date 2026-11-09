package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.user.domain.JoinCallMode;

import java.time.Instant;

public record SessionCallView(
		JoinCallMode mode,
		Long callId,
		boolean held,
		Instant scheduledAt,
		CallGuardView guard,
		CallProgressView progress
) {
}

package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

import java.time.Instant;

public record CorrectOutcomeCommand(
		AuthPrincipal principal,
		long sessionId,
		Instant correctedStopAt,
		String reasonCode,
		String note
) {
}

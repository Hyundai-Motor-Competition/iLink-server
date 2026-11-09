package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

import java.time.Instant;

public record ChildLocationCommand(
		AuthPrincipal principal,
		long sessionId,
		Instant at,
		double lat,
		double lng,
		Double accuracyM
) {
}

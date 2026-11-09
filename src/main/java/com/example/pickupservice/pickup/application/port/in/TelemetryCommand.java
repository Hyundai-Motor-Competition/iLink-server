package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;

import java.time.Instant;

public record TelemetryCommand(
		AuthPrincipal principal,
		long sessionId,
		Instant observedAt,
		int navEtaSeconds,
		int remainingMeters,
		LatLng location,
		Integer headingDeg,
		Double speedKmh,
		String gear
) {
}

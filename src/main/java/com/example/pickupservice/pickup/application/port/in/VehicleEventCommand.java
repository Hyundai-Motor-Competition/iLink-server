package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;

import java.time.Instant;

public record VehicleEventCommand(
		AuthPrincipal principal,
		long sessionId,
		String eventId,
		String type,
		Instant occurredAt,
		LatLng location,
		String door
) {
}

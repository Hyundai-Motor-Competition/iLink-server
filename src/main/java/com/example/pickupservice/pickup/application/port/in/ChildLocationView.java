package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record ChildLocationView(
		double lat,
		double lng,
		Instant at
) {
}

package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record VehicleLocationView(
		double lat,
		double lng,
		Instant at
) {
}

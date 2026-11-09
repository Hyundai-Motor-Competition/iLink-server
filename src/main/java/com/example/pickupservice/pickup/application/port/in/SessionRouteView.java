package com.example.pickupservice.pickup.application.port.in;

public record SessionRouteView(
		int routeVersion,
		Integer remainingMeters
) {
}

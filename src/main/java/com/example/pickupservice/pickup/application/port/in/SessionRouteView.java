package com.example.pickupservice.pickup.application.port.in;

public record SessionRouteView(
		int routeVersion,
		Integer remainingMeters,
		Integer totalMeters
) {

	public SessionRouteView(int routeVersion, Integer remainingMeters) {
		this(routeVersion, remainingMeters, null);
	}

	public static SessionRouteView of(int routeVersion, Integer remainingMeters, Integer totalMeters) {
		return new SessionRouteView(routeVersion, remainingMeters, totalMeters);
	}
}

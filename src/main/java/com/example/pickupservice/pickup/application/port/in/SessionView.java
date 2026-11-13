package com.example.pickupservice.pickup.application.port.in;

import java.util.List;

public record SessionView(
		long sessionId,
		int stateVersion,
		SessionStatusView status,
		SessionSpotView spot,
		SessionEtaView eta,
		SessionCallView call,
		List<SessionChildView> children,
		VehicleLocationView vehicleLocation,
		SessionRouteView route,
		SessionBoardingView boarding,
		SessionIncidentView incident
) {

	public SessionView(long sessionId, int stateVersion, SessionStatusView status, SessionSpotView spot,
			SessionEtaView eta, SessionCallView call, List<SessionChildView> children, SessionRouteView route,
			SessionBoardingView boarding, SessionIncidentView incident) {
		this(sessionId, stateVersion, status, spot, eta, call, children, null, route, boarding, incident);
	}
}

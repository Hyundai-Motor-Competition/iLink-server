package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record ConfirmRouteCommand(
		AuthPrincipal principal,
		long sessionId,
		long spotId,
		String navRouteId,
		int stateVersion,
		Integer totalMeters
) {

	public ConfirmRouteCommand(AuthPrincipal principal, long sessionId, long spotId, String navRouteId,
			int stateVersion) {
		this(principal, sessionId, spotId, navRouteId, stateVersion, null);
	}

	public static ConfirmRouteCommand of(AuthPrincipal principal, long sessionId, long spotId, String navRouteId,
			int stateVersion, Integer totalMeters) {
		return new ConfirmRouteCommand(principal, sessionId, spotId, navRouteId, stateVersion, totalMeters);
	}
}

package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record ConfirmRouteCommand(
		AuthPrincipal principal,
		long sessionId,
		Long spotId,
		String navRouteId,
		int stateVersion,
		Integer totalMeters,
		SpotCandidate candidate
) {

	public ConfirmRouteCommand(AuthPrincipal principal, long sessionId, long spotId, String navRouteId,
			int stateVersion) {
		this(principal, sessionId, spotId, navRouteId, stateVersion, null, null);
	}

	public ConfirmRouteCommand(AuthPrincipal principal, long sessionId, long spotId, String navRouteId,
			int stateVersion, Integer totalMeters) {
		this(principal, sessionId, spotId, navRouteId, stateVersion, totalMeters, null);
	}

	public static ConfirmRouteCommand ofCandidate(AuthPrincipal principal, long sessionId, SpotCandidate candidate,
			String navRouteId, int stateVersion, Integer totalMeters) {
		return new ConfirmRouteCommand(principal, sessionId, null, navRouteId, stateVersion, totalMeters, candidate);
	}
}

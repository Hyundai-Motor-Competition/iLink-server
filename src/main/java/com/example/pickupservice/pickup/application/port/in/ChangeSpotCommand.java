package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record ChangeSpotCommand(
		AuthPrincipal principal,
		long sessionId,
		Long spotId,
		int stateVersion,
		int spotVersion,
		SpotCandidate candidate,
		boolean here
) {

	public ChangeSpotCommand(AuthPrincipal principal, long sessionId, long spotId, int stateVersion,
			int spotVersion) {
		this(principal, sessionId, spotId, stateVersion, spotVersion, null, false);
	}

	public static ChangeSpotCommand ofCandidate(AuthPrincipal principal, long sessionId, SpotCandidate candidate,
			int stateVersion, int spotVersion) {
		return new ChangeSpotCommand(principal, sessionId, null, stateVersion, spotVersion, candidate, false);
	}

	public static ChangeSpotCommand ofHere(AuthPrincipal principal, long sessionId, int stateVersion,
			int spotVersion) {
		return new ChangeSpotCommand(principal, sessionId, null, stateVersion, spotVersion, null, true);
	}
}

package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record ChangeSpotCommand(
		AuthPrincipal principal,
		long sessionId,
		long spotId,
		int stateVersion,
		int spotVersion
) {
}

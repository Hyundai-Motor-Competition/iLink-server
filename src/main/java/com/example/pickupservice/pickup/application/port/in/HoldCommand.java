package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record HoldCommand(
		AuthPrincipal principal,
		long sessionId,
		int stateVersion,
		String reason
) {
}

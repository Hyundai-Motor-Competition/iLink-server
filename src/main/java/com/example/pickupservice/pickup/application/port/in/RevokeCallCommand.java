package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record RevokeCallCommand(
		AuthPrincipal principal,
		long callId,
		int stateVersion,
		String reason
) {
}

package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record OpenIncidentCommand(
		AuthPrincipal principal,
		long sessionId,
		String type,
		long childUserId,
		int stateVersion
) {
}

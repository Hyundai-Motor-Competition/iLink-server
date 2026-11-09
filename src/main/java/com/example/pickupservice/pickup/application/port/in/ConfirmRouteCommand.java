package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record ConfirmRouteCommand(
		AuthPrincipal principal,
		long sessionId,
		long spotId,
		String navRouteId,
		int stateVersion
) {
}

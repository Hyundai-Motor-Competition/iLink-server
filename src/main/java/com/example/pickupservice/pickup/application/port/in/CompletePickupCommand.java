package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

import java.util.List;

public record CompletePickupCommand(
		AuthPrincipal principal,
		long sessionId,
		String mode,
		int stateVersion,
		List<Long> notBoardedChildUserIds
) {
}

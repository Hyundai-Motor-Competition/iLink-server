package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record RejectBoardingCommand(
		AuthPrincipal principal,
		long sessionId,
		Long childUserId,
		int stateVersion
) {
}

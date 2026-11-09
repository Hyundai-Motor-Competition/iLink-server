package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.pickup.domain.SessionStatus;

public record ActiveSessionResult(
		long sessionId,
		SessionStatus status,
		int stateVersion
) {
}

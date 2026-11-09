package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record AcknowledgeCallCommand(
		AuthPrincipal principal,
		long callId
) {
}

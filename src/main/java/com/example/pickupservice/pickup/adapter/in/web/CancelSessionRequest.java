package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.CancelSessionCommand;
import jakarta.validation.constraints.NotNull;

public record CancelSessionRequest(
		@NotNull Integer stateVersion,
		String reason
) {

	public CancelSessionCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new CancelSessionCommand(principal, sessionId, stateVersion, reason);
	}

}

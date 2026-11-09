package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.HoldCommand;
import jakarta.validation.constraints.NotNull;

public record HoldSessionRequest(
		@NotNull Integer stateVersion,
		String reason
) {

	public HoldCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new HoldCommand(principal, sessionId, stateVersion, reason);
	}

}

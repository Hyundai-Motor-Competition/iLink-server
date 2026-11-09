package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.RevokeCallCommand;
import jakarta.validation.constraints.NotNull;

public record RevokeCallRequest(
		@NotNull Integer stateVersion,
		String reason
) {

	public RevokeCallCommand toCommand(AuthPrincipal principal, long callId) {
		return new RevokeCallCommand(principal, callId, stateVersion, reason);
	}

}

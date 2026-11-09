package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.RevokeCallCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevokeCallRequest(
		@NotNull Integer stateVersion,
		@Size(max = 100) String reason
) {

	public RevokeCallCommand toCommand(AuthPrincipal principal, long callId) {
		return new RevokeCallCommand(principal, callId, stateVersion, reason);
	}

}

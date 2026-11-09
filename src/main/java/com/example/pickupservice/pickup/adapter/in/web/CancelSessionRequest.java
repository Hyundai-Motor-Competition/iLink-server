package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.CancelSessionCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelSessionRequest(
		@NotNull Integer stateVersion,
		@Size(max = 100) String reason
) {

	public CancelSessionCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new CancelSessionCommand(principal, sessionId, stateVersion, reason);
	}

}

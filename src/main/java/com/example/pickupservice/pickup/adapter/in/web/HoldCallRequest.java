package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.CallHoldCommand;
import jakarta.validation.constraints.NotNull;

public record HoldCallRequest(
		@NotNull Integer stateVersion
) {

	public CallHoldCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new CallHoldCommand(principal, sessionId, stateVersion);
	}

}

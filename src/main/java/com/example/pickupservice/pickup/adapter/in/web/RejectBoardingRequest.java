package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.RejectBoardingCommand;
import jakarta.validation.constraints.NotNull;

public record RejectBoardingRequest(
		Long childUserId,
		@NotNull Integer stateVersion
) {

	public RejectBoardingCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new RejectBoardingCommand(principal, sessionId, childUserId, stateVersion);
	}

}

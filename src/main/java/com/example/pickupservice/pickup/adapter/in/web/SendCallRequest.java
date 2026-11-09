package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.SendCallCommand;
import jakarta.validation.constraints.NotNull;

public record SendCallRequest(
		@NotNull Integer stateVersion
) {

	public SendCallCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new SendCallCommand(principal, sessionId, stateVersion);
	}

}

package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.HelpRequestCommand;
import jakarta.validation.constraints.NotNull;

public record HelpRequestRequest(
		@NotNull String type
) {

	public HelpRequestCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new HelpRequestCommand(principal, sessionId, type);
	}

}

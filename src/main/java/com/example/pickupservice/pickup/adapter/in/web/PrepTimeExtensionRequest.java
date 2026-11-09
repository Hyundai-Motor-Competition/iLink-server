package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.ExtendPrepTimeCommand;
import jakarta.validation.constraints.NotNull;

public record PrepTimeExtensionRequest(
		@NotNull @ValidPrepMinutes Integer minutes
) {

	public ExtendPrepTimeCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new ExtendPrepTimeCommand(principal, sessionId, minutes);
	}

}

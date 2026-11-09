package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteCommand;
import jakarta.validation.constraints.NotNull;

public record ConfirmRouteRequest(
		@NotNull Long spotId,
		String navRouteId,
		@NotNull Integer stateVersion
) {

	public ConfirmRouteCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new ConfirmRouteCommand(principal, sessionId, spotId, navRouteId, stateVersion);
	}

}

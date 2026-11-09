package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotCommand;
import jakarta.validation.constraints.NotNull;

public record ChangeSpotRequest(
		@NotNull Long spotId,
		@NotNull Integer stateVersion,
		@NotNull Integer spotVersion
) {

	public ChangeSpotCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new ChangeSpotCommand(principal, sessionId, spotId, stateVersion, spotVersion);
	}

}

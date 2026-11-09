package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.OpenIncidentCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OpenIncidentRequest(
		@NotNull @Pattern(regexp = "CHILD_NOT_ON_BOARD") String type,
		@NotNull Long childUserId,
		@NotNull Integer stateVersion
) {

	public OpenIncidentCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new OpenIncidentCommand(principal, sessionId, type, childUserId, stateVersion);
	}

}

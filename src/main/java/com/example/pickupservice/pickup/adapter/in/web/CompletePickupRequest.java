package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.CompletePickupCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CompletePickupRequest(
		@NotNull @Pattern(regexp = "IMMEDIATE|MANUAL") String mode,
		@NotNull Integer stateVersion,
		List<Long> notBoardedChildUserIds
) {

	public CompletePickupCommand toCommand(AuthPrincipal principal, long sessionId) {
		List<Long> notBoarded = notBoardedChildUserIds == null ? List.of() : notBoardedChildUserIds;
		return new CompletePickupCommand(principal, sessionId, mode, stateVersion, notBoarded);
	}

}

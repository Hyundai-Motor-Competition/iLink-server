package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@ValidSpotChoice
public record ConfirmRouteRequest(
		Long spotId,
		@Valid SpotCandidateRequest candidate,
		String navRouteId,
		@NotNull Integer stateVersion,
		@PositiveOrZero Integer totalMeters
) {

	public ConfirmRouteCommand toCommand(AuthPrincipal principal, long sessionId) {
		if (candidate != null) {
			return ConfirmRouteCommand.ofCandidate(principal, sessionId, candidate.toCandidate(), navRouteId,
					stateVersion, totalMeters);
		}
		return new ConfirmRouteCommand(principal, sessionId, spotId, navRouteId, stateVersion, totalMeters);
	}

}

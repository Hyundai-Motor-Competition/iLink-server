package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@ValidSpotChoice
public record ChangeSpotRequest(
		Long spotId,
		@Valid SpotCandidateRequest candidate,
		Boolean here,
		@NotNull Integer stateVersion,
		@NotNull Integer spotVersion
) implements SpotChoiceRequest {

	public ChangeSpotCommand toCommand(AuthPrincipal principal, long sessionId) {
		if (Boolean.TRUE.equals(here)) {
			return ChangeSpotCommand.ofHere(principal, sessionId, stateVersion, spotVersion);
		}
		if (candidate != null) {
			return ChangeSpotCommand.ofCandidate(principal, sessionId, candidate.toCandidate(), stateVersion,
					spotVersion);
		}
		return new ChangeSpotCommand(principal, sessionId, spotId, stateVersion, spotVersion);
	}

}

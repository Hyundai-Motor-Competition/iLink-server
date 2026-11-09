package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.ChildLocationCommand;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ChildLocationRequest(
		@NotNull OffsetDateTime at,
		@NotNull Double lat,
		@NotNull Double lng,
		Double accuracyM
) {

	public ChildLocationCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new ChildLocationCommand(principal, sessionId, TimeUtils.toUtc(at), lat, lng, accuracyM);
	}

}

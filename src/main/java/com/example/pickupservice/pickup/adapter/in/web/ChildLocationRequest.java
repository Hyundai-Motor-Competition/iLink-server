package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.ChildLocationCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ChildLocationRequest(
		@NotNull OffsetDateTime at,
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
		Double accuracyM
) {

	public ChildLocationCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new ChildLocationCommand(principal, sessionId, TimeUtils.toUtc(at), lat, lng, accuracyM);
	}

}

package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.TelemetryCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record VehicleTelemetryRequest(
		@NotNull OffsetDateTime observedAt,
		@NotNull @Min(0) Integer navEtaSeconds,
		@NotNull @Min(0) Integer remainingMeters,
		LatLng location,
		Integer headingDeg,
		Double speedKmh,
		String gear
) {

	public TelemetryCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new TelemetryCommand(principal, sessionId, TimeUtils.toUtc(observedAt), navEtaSeconds, remainingMeters,
				location, headingDeg, speedKmh, gear);
	}

}

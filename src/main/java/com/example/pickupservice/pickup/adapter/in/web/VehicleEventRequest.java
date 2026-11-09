package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.VehicleEventCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

@ValidDoorCycle
public record VehicleEventRequest(
		@NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String eventId,
		@NotNull @Pattern(regexp = "^(STOPPED|DEPARTED|STOP_FAILED|DOOR_CYCLE)$") String type,
		@NotNull OffsetDateTime occurredAt,
		LatLng location,
		String door
) {

	public VehicleEventCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new VehicleEventCommand(principal, sessionId, eventId, type, TimeUtils.toUtc(occurredAt), location, door);
	}

}

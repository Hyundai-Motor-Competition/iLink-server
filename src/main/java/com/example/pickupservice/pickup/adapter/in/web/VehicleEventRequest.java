package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.VehicleEventCommand;

import java.time.OffsetDateTime;

@ValidDoorCycle
public record VehicleEventRequest(
		String eventId,
		String type,
		OffsetDateTime occurredAt,
		LatLng location,
		String door
) {

	public VehicleEventCommand toCommand(AuthPrincipal principal, long sessionId) {
		return new VehicleEventCommand(principal, sessionId, eventId, type, TimeUtils.toUtc(occurredAt), location, door);
	}

}

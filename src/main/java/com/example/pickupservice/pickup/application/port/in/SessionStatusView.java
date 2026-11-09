package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;

public record SessionStatusView(
		SessionStatus session,
		VehiclePhase vehicle,
		String call,
		SpotStatus spot
) {
}

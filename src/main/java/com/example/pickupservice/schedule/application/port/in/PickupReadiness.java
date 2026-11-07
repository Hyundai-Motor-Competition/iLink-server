package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.MissingSetupItem;

import java.util.List;

public record PickupReadiness(
		boolean startable,
		List<MissingSetupItem> missing
) {
}

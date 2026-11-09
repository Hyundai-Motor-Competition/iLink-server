package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.JoinCallMode;

import java.util.List;

public record VehicleSettingsSummary(
		String guardianNameMasked,
		List<VehicleChild> children,
		JoinCallMode joinCallMode
) {
}

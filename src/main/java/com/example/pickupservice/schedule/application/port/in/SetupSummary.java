package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.JoinCallMode;

import java.util.List;

public record SetupSummary(
		SetupChild child,
		List<SetupSchedule> schedules,
		List<SetupSpot> pickupSpots,
		JoinCallMode joinCallMode,
		List<MissingSetupItem> missing,
		int settingsVersion
) {
}

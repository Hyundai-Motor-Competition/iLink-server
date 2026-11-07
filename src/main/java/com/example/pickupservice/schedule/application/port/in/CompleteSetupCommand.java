package com.example.pickupservice.schedule.application.port.in;

public record CompleteSetupCommand(
		long familyId,
		long childId,
		int settingsVersion
) {
}

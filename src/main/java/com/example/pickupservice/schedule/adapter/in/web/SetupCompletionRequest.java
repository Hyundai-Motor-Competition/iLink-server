package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.CompleteSetupCommand;
import jakarta.validation.constraints.NotNull;

public record SetupCompletionRequest(
		@NotNull Integer settingsVersion
) {

	public CompleteSetupCommand toCommand(long familyId, long childId) {
		return new CompleteSetupCommand(familyId, childId, settingsVersion);
	}
}

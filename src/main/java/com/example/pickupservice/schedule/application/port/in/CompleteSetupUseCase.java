package com.example.pickupservice.schedule.application.port.in;

public interface CompleteSetupUseCase {

	SetupCompletionResult completeSetup(CompleteSetupCommand command);
}

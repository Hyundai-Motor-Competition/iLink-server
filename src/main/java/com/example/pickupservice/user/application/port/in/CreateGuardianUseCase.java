package com.example.pickupservice.user.application.port.in;

public interface CreateGuardianUseCase {

	GuardianAccount createGuardian(CreateGuardianCommand command);

}

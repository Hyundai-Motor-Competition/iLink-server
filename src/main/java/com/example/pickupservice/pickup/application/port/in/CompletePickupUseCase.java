package com.example.pickupservice.pickup.application.port.in;

public interface CompletePickupUseCase {

	SessionView complete(CompletePickupCommand command);

}

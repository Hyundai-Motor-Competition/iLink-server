package com.example.pickupservice.pickup.application.port.in;

public interface CancelSessionUseCase {

	SessionView cancel(CancelSessionCommand command);

}

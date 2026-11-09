package com.example.pickupservice.pickup.application.port.in;

public interface HoldSessionUseCase {

	SessionView hold(HoldCommand command);

	SessionView releaseHold(HoldCommand command);

}

package com.example.pickupservice.pickup.application.port.in;

public interface HoldCallUseCase {

	SessionView holdCall(CallHoldCommand command);

	SessionView releaseCallHold(CallHoldCommand command);

}

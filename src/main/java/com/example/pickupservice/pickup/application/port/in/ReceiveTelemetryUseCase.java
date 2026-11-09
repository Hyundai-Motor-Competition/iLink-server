package com.example.pickupservice.pickup.application.port.in;

public interface ReceiveTelemetryUseCase {

	void receive(TelemetryCommand command);

}

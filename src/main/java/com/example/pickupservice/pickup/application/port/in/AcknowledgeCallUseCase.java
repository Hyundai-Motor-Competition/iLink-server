package com.example.pickupservice.pickup.application.port.in;

public interface AcknowledgeCallUseCase {

	AcknowledgeResult acknowledge(AcknowledgeCallCommand command);

}

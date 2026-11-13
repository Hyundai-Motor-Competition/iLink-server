package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public interface RevokeSignalLostCallsUseCase {

	int revokeSignalLostCalls(Instant now);

}

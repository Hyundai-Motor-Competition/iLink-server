package com.example.pickupservice.notification.application.port.in;

import java.time.Instant;

public interface ResendUndeliveredUseCase {

	int resendUndelivered(Instant now);
}

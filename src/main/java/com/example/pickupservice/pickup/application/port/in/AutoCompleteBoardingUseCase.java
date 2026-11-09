package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public interface AutoCompleteBoardingUseCase {

	int completeExpiredBoardings(Instant now);

}

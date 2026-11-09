package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record PrepExtensionResult(
		int extraPrepMinutes,
		Instant callNotBefore
) {
}

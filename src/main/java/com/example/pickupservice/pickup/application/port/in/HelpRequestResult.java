package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public record HelpRequestResult(
		Instant nextAllowedAt
) {
}

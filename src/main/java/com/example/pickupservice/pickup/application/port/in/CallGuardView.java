package com.example.pickupservice.pickup.application.port.in;

import java.util.List;

public record CallGuardView(
		boolean met,
		List<String> unmet
) {
}

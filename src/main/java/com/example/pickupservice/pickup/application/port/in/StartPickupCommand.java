package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

import java.time.LocalDate;
import java.util.List;

public record StartPickupCommand(
		AuthPrincipal principal,
		List<Long> scheduleIds,
		LocalDate date
) {
}

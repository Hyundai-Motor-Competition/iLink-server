package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.StartPickupCommand;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record StartPickupRequest(
		@NotEmpty List<Long> scheduleIds,
		LocalDate date
) {

	public StartPickupCommand toCommand(AuthPrincipal principal) {
		return new StartPickupCommand(principal, scheduleIds, date);
	}

}

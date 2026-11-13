package com.example.pickupservice.user.application.port.in;

public record ChangeChildLocationCommand(
		long familyId,
		boolean enabled
) {
}

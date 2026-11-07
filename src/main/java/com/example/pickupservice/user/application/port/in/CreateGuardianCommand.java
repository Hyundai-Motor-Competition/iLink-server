package com.example.pickupservice.user.application.port.in;

public record CreateGuardianCommand(
		long vehicleId,
		String phone,
		String name
) {
}

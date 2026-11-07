package com.example.pickupservice.user.application.port.in;

public record UnlinkVehicleCommand(
		long familyId,
		long vehicleId
) {
}

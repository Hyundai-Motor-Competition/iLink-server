package com.example.pickupservice.user.application.port.in;

public record VehicleInfo(
		long vehicleId,
		Long familyId,
		String vin,
		String model,
		String plateNumber,
		boolean linked
) {
}

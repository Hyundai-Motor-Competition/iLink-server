package com.example.pickupservice.user.application.port.in;

public record VehicleSummary(
		long vehicleId,
		String model,
		String plateNumberMasked
) {
}

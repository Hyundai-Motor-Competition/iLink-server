package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.user.application.port.in.VehicleSummary;

public record VehicleResponse(
		long vehicleId,
		String model,
		String plateNumberMasked
) {

	public static VehicleResponse from(VehicleSummary vehicle) {
		if (vehicle == null) {
			return null;
		}
		return new VehicleResponse(vehicle.vehicleId(), vehicle.model(), vehicle.plateNumberMasked());
	}

}

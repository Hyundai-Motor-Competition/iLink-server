package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.VehicleDeviceResult;

public record VehicleDeviceResponse(
		long deviceId,
		long vehicleId,
		String deviceToken
) {

	public static VehicleDeviceResponse from(VehicleDeviceResult result) {
		return new VehicleDeviceResponse(result.deviceId(), result.vehicleId(), result.deviceToken());
	}

}

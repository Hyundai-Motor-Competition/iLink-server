package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.VehicleLinkResult;

public record VehicleLinkResponse(
		long vehicleId,
		long familyId,
		long userId,
		String accessToken,
		String refreshToken
) {

	public static VehicleLinkResponse from(VehicleLinkResult result) {
		return new VehicleLinkResponse(
				result.vehicleId(),
				result.familyId(),
				result.userId(),
				result.accessToken(),
				result.refreshToken());
	}

}

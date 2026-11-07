package com.example.pickupservice.auth.application.port.in;

public record LinkStatusResult(
		LinkStatus status,
		Long vehicleId,
		Long familyId,
		String accessToken,
		String refreshToken,
		Integer pollAfterSeconds) {
}

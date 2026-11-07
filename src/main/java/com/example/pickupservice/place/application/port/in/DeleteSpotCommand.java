package com.example.pickupservice.place.application.port.in;

public record DeleteSpotCommand(
		long familyId,
		long spotId
) {
}

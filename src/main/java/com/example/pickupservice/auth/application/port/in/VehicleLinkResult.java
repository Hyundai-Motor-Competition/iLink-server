package com.example.pickupservice.auth.application.port.in;

public record VehicleLinkResult(long vehicleId, long familyId, long userId, String accessToken, String refreshToken) {
}

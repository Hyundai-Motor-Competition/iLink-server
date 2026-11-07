package com.example.pickupservice.auth.application.port.in;

public record VehicleDeviceResult(long deviceId, long vehicleId, String deviceToken) {
}

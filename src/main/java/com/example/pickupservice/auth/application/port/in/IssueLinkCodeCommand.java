package com.example.pickupservice.auth.application.port.in;

public record IssueLinkCodeCommand(long deviceId, long vehicleId, Long familyId) {
}

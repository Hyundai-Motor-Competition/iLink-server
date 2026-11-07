package com.example.pickupservice.auth.application.port.out;

public interface CheckActivePickupPort {

	boolean hasActivePickup(long vehicleId);
}

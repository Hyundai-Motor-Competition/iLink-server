package com.example.pickupservice.user.application.port.out;

public interface CheckActivePickupPort {

	boolean isInActivePickup(long userId);

	boolean hasActivePickup(long vehicleId);
}

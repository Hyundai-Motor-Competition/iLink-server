package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

import java.util.Optional;

public interface ActivePickupQueryUseCase {

	boolean hasActivePickup(long vehicleId);

	boolean isInActivePickup(long userId);

	boolean isSpotInUse(long spotId);

	Optional<LatLng> activeChildLocation(long familyId);

}

package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;

import java.util.Optional;

public interface CheckActivePickupPort {

	boolean isUsedByActiveSession(long spotId);

	Optional<LatLng> loadActiveChildLocation(long familyId);
}

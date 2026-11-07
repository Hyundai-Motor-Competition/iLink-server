package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.domain.PickupSpot;

import java.util.List;
import java.util.Optional;

public interface LoadSpotPort {

	Optional<PickupSpot> loadSpot(long spotId);

	List<PickupSpot> loadSpots(long familyId, Long academyId);

	List<PickupSpot> loadSpotsWithin(long familyId, Long academyId, LatLng near, int radiusM);

	Optional<PickupSpot> loadDefaultSpot(long familyId, long academyId);
}

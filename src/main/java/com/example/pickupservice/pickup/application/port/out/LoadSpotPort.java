package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.place.application.port.in.SpotInfo;

import java.util.Optional;

public interface LoadSpotPort {

	Optional<SpotInfo> loadDefaultSpot(long familyId, long academyId);

	Optional<SpotInfo> loadSpot(long spotId);

}

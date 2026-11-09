package com.example.pickupservice.place.application.port.in;

import java.util.List;
import java.util.Optional;

public interface LoadPickupSpotUseCase {

	Optional<SpotInfo> findDefaultSpot(long familyId, long academyId);

	Optional<SpotInfo> findSpot(long spotId);

	List<SpotSummary> findSpots(long familyId, List<Long> academyIds);
}

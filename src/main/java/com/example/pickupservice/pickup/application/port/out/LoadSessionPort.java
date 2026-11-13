package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.PickupSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoadSessionPort {

	Optional<PickupSession> load(long sessionId);

	Optional<PickupSession> loadForUpdate(long sessionId);

	Optional<PickupSession> loadActiveByVehicle(long vehicleId);

	Optional<PickupSession> loadActiveByFamily(long familyId);

	Optional<PickupSession> loadActiveByChild(long childUserId);

	Optional<PickupSession> loadActiveBySpot(long spotId);

	List<PickupSession> loadBoardingExpired(Instant now);

	List<PickupSession> loadSignalLost(Instant staleBefore);

}

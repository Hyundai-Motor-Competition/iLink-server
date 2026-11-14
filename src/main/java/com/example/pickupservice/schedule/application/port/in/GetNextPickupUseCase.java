package com.example.pickupservice.schedule.application.port.in;

import java.util.Optional;

public interface GetNextPickupUseCase {

	Optional<NextPickup> getNextPickup(long familyId, long childUserId);
}

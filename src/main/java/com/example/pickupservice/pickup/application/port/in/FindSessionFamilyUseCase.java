package com.example.pickupservice.pickup.application.port.in;

import java.util.Optional;

public interface FindSessionFamilyUseCase {

	Optional<Long> findFamilyId(long sessionId);

}

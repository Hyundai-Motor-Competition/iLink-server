package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.PickupOutcome;

import java.util.Optional;

public interface LoadOutcomePort {

	Optional<PickupOutcome> loadOutcome(long sessionId);

}

package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.PickupOutcome;

public interface SaveOutcomePort {

	PickupOutcome saveOutcome(PickupOutcome outcome);

}

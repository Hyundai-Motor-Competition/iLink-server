package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.PickupSession;

public interface SaveSessionPort {

	PickupSession save(PickupSession session);

}

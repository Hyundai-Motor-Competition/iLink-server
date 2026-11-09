package com.example.pickupservice.user.adapter.out.pickup;

import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import org.springframework.stereotype.Component;

@Component
class PickupActivityAdapter implements CheckActivePickupPort {

	@Override
	public boolean isInActivePickup(long userId) {
		return false;
	}

	@Override
	public boolean hasActivePickup(long vehicleId) {
		return false;
	}
}

package com.example.pickupservice.auth.adapter.out.pickup;

import com.example.pickupservice.auth.application.port.out.CheckActivePickupPort;
import org.springframework.stereotype.Component;

// pickup 구현 전 임시 동작: 진행 중 픽업이 없다고 본다
@Component
class AuthPickupActivityAdapter implements CheckActivePickupPort {

	@Override
	public boolean hasActivePickup(long vehicleId) {
		return false;
	}
}

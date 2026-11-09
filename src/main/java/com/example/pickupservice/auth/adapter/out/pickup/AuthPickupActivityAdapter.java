package com.example.pickupservice.auth.adapter.out.pickup;

import com.example.pickupservice.auth.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.pickup.application.port.in.ActivePickupQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// pickup의 ActivePickupQueryUseCase에 위임한다
@Component
@RequiredArgsConstructor
class AuthPickupActivityAdapter implements CheckActivePickupPort {

	private final ActivePickupQueryUseCase activePickupQuery;

	@Override
	public boolean hasActivePickup(long vehicleId) {
		return activePickupQuery.hasActivePickup(vehicleId);
	}
}

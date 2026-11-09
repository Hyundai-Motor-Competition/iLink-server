package com.example.pickupservice.user.adapter.out.pickup;

import com.example.pickupservice.pickup.application.port.in.ActivePickupQueryUseCase;
import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// pickup의 ActivePickupQueryUseCase에 위임한다
@Component
@RequiredArgsConstructor
class PickupActivityAdapter implements CheckActivePickupPort {

	private final ActivePickupQueryUseCase activePickupQuery;

	@Override
	public boolean isInActivePickup(long userId) {
		return activePickupQuery.isInActivePickup(userId);
	}

	@Override
	public boolean hasActivePickup(long vehicleId) {
		return activePickupQuery.hasActivePickup(vehicleId);
	}
}

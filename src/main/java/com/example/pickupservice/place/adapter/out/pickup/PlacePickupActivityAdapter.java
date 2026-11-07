package com.example.pickupservice.place.adapter.out.pickup;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

// pickup 구현 전 임시 동작: 진행 중 세션이 없다고 본다
@Component
class PlacePickupActivityAdapter implements CheckActivePickupPort {

	@Override
	public boolean isUsedByActiveSession(long spotId) {
		return false;
	}

	@Override
	public Optional<LatLng> loadActiveChildLocation(long familyId) {
		return Optional.empty();
	}
}

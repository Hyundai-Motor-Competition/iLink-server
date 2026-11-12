package com.example.pickupservice.place.adapter.out.pickup;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.in.ActivePickupQueryUseCase;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlacePickupActivityAdapter implements CheckActivePickupPort {

	private final ActivePickupQueryUseCase activePickupQuery;

	@Override
	public boolean isUsedByActiveSession(long spotId) {
		return activePickupQuery.isSpotInUse(spotId);
	}

	@Override
	public Optional<LatLng> loadActiveChildLocation(long familyId) {
		return activePickupQuery.activeChildLocation(familyId);
	}
}

package com.example.pickupservice.pickup.adapter.out.place;

import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.in.LoadPickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PickupPlaceSpotAdapter implements LoadSpotPort {

	private final LoadPickupSpotUseCase loadPickupSpotUseCase;

	@Override
	public Optional<SpotInfo> loadDefaultSpot(long familyId, long academyId) {
		return loadPickupSpotUseCase.findDefaultSpot(familyId, academyId);
	}

	@Override
	public Optional<SpotInfo> loadSpot(long spotId) {
		return loadPickupSpotUseCase.findSpot(spotId);
	}
}

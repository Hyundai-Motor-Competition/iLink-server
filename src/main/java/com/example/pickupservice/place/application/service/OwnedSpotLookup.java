package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;

final class OwnedSpotLookup {

	private OwnedSpotLookup() {
	}

	static PickupSpot load(LoadSpotPort loadSpotPort, long familyId, long spotId) {
		return loadSpotPort.loadSpot(spotId)
				.filter(spot -> spot.getFamilyId() == familyId)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
	}
}

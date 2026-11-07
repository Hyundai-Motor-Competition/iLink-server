package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;

/** 내 가족의 장소를 읽는다. 없거나 다른 가족의 것이면 404 NOT_FOUND. 수정 · 삭제가 같이 쓴다. */
final class OwnedSpotLookup {

	private OwnedSpotLookup() {
	}

	static PickupSpot load(LoadSpotPort loadSpotPort, long familyId, long spotId) {
		return loadSpotPort.loadSpot(spotId)
				.filter(spot -> spot.getFamilyId() == familyId)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
	}
}

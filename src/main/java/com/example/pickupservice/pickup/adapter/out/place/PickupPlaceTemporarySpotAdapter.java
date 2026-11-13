package com.example.pickupservice.pickup.adapter.out.place;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.out.SaveTemporarySpotPort;
import com.example.pickupservice.place.application.port.in.SaveTemporarySpotCommand;
import com.example.pickupservice.place.application.port.in.SaveTemporarySpotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PickupPlaceTemporarySpotAdapter implements SaveTemporarySpotPort {

	private final SaveTemporarySpotUseCase saveTemporarySpotUseCase;

	@Override
	public long saveTemporarySpot(long familyId, long academyId, String name, LatLng stopPoint, LatLng meetingPoint) {
		return saveTemporarySpotUseCase.saveTemporarySpot(
				new SaveTemporarySpotCommand(familyId, academyId, name, stopPoint, meetingPoint));
	}
}

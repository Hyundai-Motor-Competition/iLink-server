package com.example.pickupservice.place.application.port.in;

/** 차량 세션용 임시 장소(temporary=true인 PickupSpot)를 저장하고 spotId를 돌려준다. */
public interface SaveTemporarySpotUseCase {

	long saveTemporarySpot(SaveTemporarySpotCommand command);
}

package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.place.domain.PickupSpot;

public interface SaveSpotPort {

	PickupSpot saveSpot(PickupSpot spot);

	void deleteSpot(long spotId);
}

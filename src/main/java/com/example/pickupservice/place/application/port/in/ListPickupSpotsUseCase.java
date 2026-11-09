package com.example.pickupservice.place.application.port.in;

public interface ListPickupSpotsUseCase {

	SpotGroups listSpots(ListSpotsQuery query);
}

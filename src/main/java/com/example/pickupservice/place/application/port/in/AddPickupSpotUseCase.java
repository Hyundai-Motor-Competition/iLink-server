package com.example.pickupservice.place.application.port.in;

public interface AddPickupSpotUseCase {

	SpotCreated addSpot(AddSpotCommand command);
}

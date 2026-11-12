package com.example.pickupservice.place.application.port.in;

public interface RecommendPickupSpotsUseCase {

	SpotRecommendations recommendSpots(RecommendSpotsQuery q);
}

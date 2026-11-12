package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.place.application.port.in.SpotRecommendations;

import java.util.List;

public record SpotRecommendationsResponse(boolean supported, List<RecommendedSpotResponse> items) {

	public static SpotRecommendationsResponse from(SpotRecommendations recommendations) {
		return new SpotRecommendationsResponse(recommendations.supported(),
				recommendations.items().stream().map(RecommendedSpotResponse::from).toList());
	}
}

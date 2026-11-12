package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.place.domain.RecommendedSpot;

import java.util.List;

public record SpotRecommendations(boolean supported, List<RecommendedSpot> items) {
}

package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.domain.RecommendedSpot;

import java.util.List;

public record RecommendedSpotResponse(
		int rank,
		String kind,
		String name,
		LatLng location,
		int walkMeters,
		int walkSeconds,
		Integer driveSeconds,
		Integer totalSeconds,
		List<String> cautions
) {

	public static RecommendedSpotResponse from(RecommendedSpot spot) {
		return new RecommendedSpotResponse(spot.rank(), spot.candidate().kind().name(), spot.candidate().name(),
				spot.candidate().location(), spot.candidate().walkMeters(), spot.candidate().walkSeconds(),
				spot.driveSeconds(), spot.totalSeconds(), spot.cautions().stream().map(Enum::name).toList());
	}
}

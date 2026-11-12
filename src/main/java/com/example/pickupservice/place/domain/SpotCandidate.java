package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;

public record SpotCandidate(
		SpotKind kind,
		String name,
		LatLng location,
		int walkMeters,
		int walkSeconds,
		LineKind lineKind,
		boolean nearEnforcementCctv) {
}

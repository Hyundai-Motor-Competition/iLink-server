package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;

public record CurbStopCandidate(
		long candidateId,
		LatLng location,
		String roadName,
		LineKind lineKind,
		boolean nearEnforcementCctv) {
}

package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record AcademyCandidate(
		String externalPlaceId,
		String name,
		String address,
		LatLng location
) {
}

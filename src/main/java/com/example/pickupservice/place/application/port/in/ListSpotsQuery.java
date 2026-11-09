package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record ListSpotsQuery(
		long familyId,
		Long academyId,
		LatLng near,
		Integer radiusM
) {
}

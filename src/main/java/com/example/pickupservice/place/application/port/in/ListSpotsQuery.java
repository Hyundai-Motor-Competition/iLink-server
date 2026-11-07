package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

/** academyId · near · radiusM은 선택(null = 안 보냄). */
public record ListSpotsQuery(
		long familyId,
		Long academyId,
		LatLng near,
		Integer radiusM
) {
}

package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;

public record AcademyInfo(
		long academyId,
		String name,
		LatLng location
) {
}

package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record AcademyInfo(
		long academyId,
		String name,
		String address,
		LatLng location,
		LatLng entrance,
		String phone
) {
}

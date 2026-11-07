package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record SpotInfo(
		long spotId,
		long familyId,
		long academyId,
		String name,
		LatLng stopPoint,
		LatLng meetingPoint
) {
}

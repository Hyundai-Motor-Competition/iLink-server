package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record AddSpotCommand(
		long familyId,
		long createdBy,
		long academyId,
		String name,
		LatLng stopPoint,
		LatLng meetingPoint,
		String memo,
		boolean isDefault
) {
}

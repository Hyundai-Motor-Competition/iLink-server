package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record UpdateSpotCommand(
		long familyId,
		long spotId,
		String name,
		LatLng stopPoint,
		LatLng meetingPoint,
		String memo,
		Boolean isDefault
) {
}

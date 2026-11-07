package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

/** 보낸 필드만 바꾼다. null = 안 보냄. */
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

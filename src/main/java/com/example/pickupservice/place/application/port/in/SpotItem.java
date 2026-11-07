package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

/** walkMinutes는 진행 중 세션이 없으면 null. */
public record SpotItem(
		long spotId,
		String name,
		LatLng stopPoint,
		LatLng meetingPoint,
		String memo,
		boolean isDefault,
		Integer walkMinutes
) {
}

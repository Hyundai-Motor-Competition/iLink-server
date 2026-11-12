package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;

public record PublicParking(
		String parkingId,
		String name,
		LatLng location,
		Integer spaces,
		Integer baseMinutes,
		Integer baseFeeWon) {
}

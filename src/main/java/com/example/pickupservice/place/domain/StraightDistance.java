package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;

public final class StraightDistance {

	private static final double EARTH_RADIUS_METERS = 6_371_000;

	private StraightDistance() {
	}

	public static double meters(LatLng from, LatLng to) {
		double lat1 = Math.toRadians(from.lat());
		double lat2 = Math.toRadians(to.lat());
		double dLat = lat2 - lat1;
		double dLng = Math.toRadians(to.lng() - from.lng());
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
	}
}

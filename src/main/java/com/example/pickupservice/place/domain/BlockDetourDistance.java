package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;

public final class BlockDetourDistance {

	private static final double EARTH_RADIUS_METERS = 6_371_000;

	private BlockDetourDistance() {
	}

	public static double meters(LatLng from, LatLng to) {
		double northSouth = EARTH_RADIUS_METERS * Math.toRadians(Math.abs(to.lat() - from.lat()));
		double meanLat = Math.toRadians((from.lat() + to.lat()) / 2);
		double eastWest = EARTH_RADIUS_METERS * Math.cos(meanLat) * Math.toRadians(Math.abs(to.lng() - from.lng()));
		return northSouth + eastWest;
	}
}

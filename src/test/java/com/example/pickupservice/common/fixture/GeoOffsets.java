package com.example.pickupservice.common.fixture;

import com.example.pickupservice.common.geo.LatLng;

public final class GeoOffsets {

	private static final double WGS84_SEMI_MAJOR_AXIS_METERS = 6_378_137.0;
	private static final double WGS84_ECCENTRICITY_SQUARED = 0.00669437999014;

	private GeoOffsets() {
	}

	public static LatLng northOf(LatLng origin, double meters) {
		double sinLatitude = Math.sin(Math.toRadians(origin.lat()));
		double meridianRadiusOfCurvature = WGS84_SEMI_MAJOR_AXIS_METERS * (1 - WGS84_ECCENTRICITY_SQUARED)
				/ Math.pow(1 - WGS84_ECCENTRICITY_SQUARED * sinLatitude * sinLatitude, 1.5);
		double deltaLatDegrees = Math.toDegrees(meters / meridianRadiusOfCurvature);
		return new LatLng(origin.lat() + deltaLatDegrees, origin.lng());
	}
}

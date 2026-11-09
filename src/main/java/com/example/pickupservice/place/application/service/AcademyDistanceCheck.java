package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.DistancePort;

final class AcademyDistanceCheck {

	static final int MAX_DISTANCE_METERS = 500;

	private AcademyDistanceCheck() {
	}

	static void requireNear(DistancePort distancePort, LatLng academyLocation, LatLng point) {
		if (!distancePort.isWithinMeters(academyLocation, point, MAX_DISTANCE_METERS)) {
			throw PlaceErrors.spotTooFar();
		}
	}
}

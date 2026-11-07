package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.DistancePort;

/** 장소 좌표가 학원에서 500m 안인지 검사한다. 추가 · 수정이 같이 쓴다. */
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

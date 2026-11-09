package com.example.pickupservice.common.fixture;

import com.example.pickupservice.common.geo.LatLng;

/**
 * 기준 좌표에서 정북으로 meters만큼 떨어진 좌표를 만든다(경도는 그대로).
 *
 * <p>계산식: WGS84 타원체의 자오선 곡률 반지름
 * M(φ) = a(1 − e²) / (1 − e² · sin²φ)^(3/2), 위도 차이(도) = meters / M(φ) × 180 / π.
 * a = 6378137m, e² = 0.00669437999014. 위도 37.5°에서 M ≈ 6,359,088m → 1° ≈ 110,986m.
 *
 * <p>PostGIS geography 거리(ST_DWithin · ST_Distance)는 기본이 타원체 기준이라 이 값과 맞는다.
 * 구 기준(평균 반지름 6,371,008.8m, 1° ≈ 111,195m)으로 재면 같은 좌표가 약 0.19% 길게 나온다
 * (499m → 약 499.94m, 501m → 약 501.94m). 그래서 경계 500m에 499m · 501m를 쓰면 어느 쪽으로 재도
 * 안 · 밖이 바뀌지 않는다. 반대로 구 기준 1° ≈ 111,195m로 좌표를 만들면 501m가 타원체로 약 500.06m가 돼 경계에 너무 가깝다.
 */
public final class GeoOffsets {

	private static final double WGS84_A = 6_378_137.0;
	private static final double WGS84_E2 = 0.00669437999014;

	private GeoOffsets() {
	}

	public static LatLng northOf(LatLng origin, double meters) {
		double phi = Math.toRadians(origin.lat());
		double sin = Math.sin(phi);
		double meridianRadius = WGS84_A * (1 - WGS84_E2) / Math.pow(1 - WGS84_E2 * sin * sin, 1.5);
		double deltaLatDegrees = Math.toDegrees(meters / meridianRadius);
		return new LatLng(origin.lat() + deltaLatDegrees, origin.lng());
	}
}

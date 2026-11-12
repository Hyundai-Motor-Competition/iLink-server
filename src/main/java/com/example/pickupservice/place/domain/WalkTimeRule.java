package com.example.pickupservice.place.domain;

public final class WalkTimeRule {

	private WalkTimeRule() {
	}

	public static WalkTime fromStraightMeters(double straightMeters) {
		int walkMeters = (int) Math.round(straightMeters * 1.3);
		int walkSeconds = (walkMeters * 10 + 10) / 11;
		return new WalkTime(walkMeters, walkSeconds);
	}
}

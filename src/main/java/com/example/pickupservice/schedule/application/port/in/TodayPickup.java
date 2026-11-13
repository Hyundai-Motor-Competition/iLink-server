package com.example.pickupservice.schedule.application.port.in;

import java.time.Instant;

public record TodayPickup(
		long scheduleId,
		String childName,
		String academyName,
		Instant targetAt,
		TodayPickupStatus status
) {
}

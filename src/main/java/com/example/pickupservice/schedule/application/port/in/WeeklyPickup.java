package com.example.pickupservice.schedule.application.port.in;

import java.time.LocalTime;

public record WeeklyPickup(
		long scheduleId,
		String childName,
		String academyName,
		LocalTime classEndTime,
		LocalTime targetPickupTime
) {
}

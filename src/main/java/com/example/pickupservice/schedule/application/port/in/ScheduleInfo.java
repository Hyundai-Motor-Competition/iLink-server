package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.ScheduleStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record ScheduleInfo(
		long scheduleId,
		long childUserId,
		AcademyInfo academy,
		Set<DayOfWeek> weekdays,
		LocalTime classEndTime,
		int targetPickup,
		ScheduleStatus status
) {
}

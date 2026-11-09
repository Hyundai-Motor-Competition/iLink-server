package com.example.pickupservice.schedule.application.port.in;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record UpdateScheduleCommand(
		long familyId,
		long scheduleId,
		AcademyInput academy,
		Set<DayOfWeek> weekdays,
		LocalTime classEndTime,
		int targetPickup
) {
}

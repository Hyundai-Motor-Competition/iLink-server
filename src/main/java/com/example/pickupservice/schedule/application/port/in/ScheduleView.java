package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.ScheduleStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record ScheduleView(
		long scheduleId,
		ScheduleAcademy academy,
		List<DayOfWeek> weekdays,
		LocalTime classEndTime,
		int targetPickup,
		ScheduleStatus status
) {
}

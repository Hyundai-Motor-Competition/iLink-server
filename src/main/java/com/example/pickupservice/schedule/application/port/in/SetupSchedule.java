package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.ScheduleStatus;

public record SetupSchedule(
		long scheduleId,
		String academyName,
		ScheduleStatus status
) {
}

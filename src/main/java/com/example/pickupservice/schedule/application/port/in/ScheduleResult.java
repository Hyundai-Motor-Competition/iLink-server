package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.ScheduleStatus;

public record ScheduleResult(
		long scheduleId,
		long academyId,
		ScheduleStatus status
) {
}

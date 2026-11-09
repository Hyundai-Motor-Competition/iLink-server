package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.domain.ScheduleStatus;

public record ScheduleResponse(
		long scheduleId,
		long academyId,
		ScheduleStatus status
) {

	static ScheduleResponse from(ScheduleResult result) {
		return new ScheduleResponse(result.scheduleId(), result.academyId(), result.status());
	}
}

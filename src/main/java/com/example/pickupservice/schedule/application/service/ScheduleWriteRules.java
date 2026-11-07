package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.DailyScheduleLimitRule;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleOverlapRule;

import java.util.List;

final class ScheduleWriteRules {

	private ScheduleWriteRules() {
	}

	static Academy newAcademy(AcademyInput input) {
		return Academy.create(input.name(), input.address(), input.location(), input.entrance(), input.phone());
	}

	static void rejectConflicts(PickupSchedule candidate, List<PickupSchedule> existingSchedules) {
		List<Long> overlapScheduleIds = ScheduleOverlapRule.findOverlapScheduleIds(candidate, existingSchedules);
		if (!overlapScheduleIds.isEmpty()) {
			throw ScheduleErrors.scheduleOverlap(overlapScheduleIds);
		}
		if (DailyScheduleLimitRule.exceedsLimit(candidate, existingSchedules)) {
			throw ScheduleErrors.dailyScheduleLimit();
		}
	}
}

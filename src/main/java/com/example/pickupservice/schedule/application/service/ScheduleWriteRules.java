package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.DailyScheduleLimitRule;
import com.example.pickupservice.schedule.domain.InvalidScheduleException;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleOverlapRule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

final class ScheduleWriteRules {

	private ScheduleWriteRules() {
	}

	static Academy academyOf(LoadAcademyPort loadAcademyPort, AcademyInput input) {
		return loadAcademyPort.loadAcademyByExternalPlaceId(input.externalPlaceId())
				.map(existing -> existing.updateInfo(input.name(), input.address(), input.location(), input.entrance(),
						input.phone()))
				.orElseGet(() -> Academy.create(input.externalPlaceId(), input.name(), input.address(),
						input.location(), input.entrance(), input.phone()));
	}

	static PickupSchedule newSchedule(long childUserId, Academy academy, Set<DayOfWeek> weekdays,
			LocalTime classEndTime, int targetPickup) {
		try {
			return PickupSchedule.create(childUserId, academy, weekdays, classEndTime, targetPickup);
		} catch (InvalidScheduleException e) {
			throw BusinessException.invalidField(e.field(), e.reason());
		}
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

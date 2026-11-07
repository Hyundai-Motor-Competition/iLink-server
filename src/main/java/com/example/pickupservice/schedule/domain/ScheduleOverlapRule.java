package com.example.pickupservice.schedule.domain;

import java.util.Collections;
import java.util.List;

public final class ScheduleOverlapRule {

	public static List<Long> findOverlapScheduleIds(PickupSchedule candidate, List<PickupSchedule> existingSchedules) {
		return existingSchedules.stream()
				.filter(existing -> existing.getChildUserId() == candidate.getChildUserId())
				.filter(existing -> existing.getClassEndTime().equals(candidate.getClassEndTime()))
				.filter(existing -> !Collections.disjoint(existing.getWeekdays(), candidate.getWeekdays()))
				.map(PickupSchedule::getScheduleId)
				.toList();
	}
}

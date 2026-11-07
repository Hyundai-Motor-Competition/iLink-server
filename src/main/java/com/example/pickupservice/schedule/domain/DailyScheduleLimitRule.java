package com.example.pickupservice.schedule.domain;

import java.util.List;

public final class DailyScheduleLimitRule {

	public static final int DAILY_LIMIT = 3;

	public static boolean exceedsLimit(PickupSchedule candidate, List<PickupSchedule> existingSchedules) {
		return candidate.getWeekdays().stream()
				.anyMatch(weekday -> existingSchedules.stream()
						.filter(existing -> existing.getChildUserId() == candidate.getChildUserId())
						.filter(existing -> existing.getWeekdays().contains(weekday))
						.count() >= DAILY_LIMIT);
	}
}

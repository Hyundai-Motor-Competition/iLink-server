package com.example.pickupservice.schedule.domain;

import com.example.pickupservice.common.error.BusinessException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PickupSchedule {

	private static final int MIN_TARGET_PICKUP = 0;
	private static final int MAX_TARGET_PICKUP = 60;

	private final Long scheduleId;
	private final long childUserId;
	private final Academy academy;
	private final Set<DayOfWeek> weekdays;
	private final LocalTime classEndTime;
	private final int targetPickup;
	private final ScheduleStatus status;

	public static PickupSchedule create(long childUserId, Academy academy, Set<DayOfWeek> weekdays,
			LocalTime classEndTime, int targetPickup) {
		validateWeekdays(weekdays);
		validateTargetPickup(targetPickup);
		return new PickupSchedule(null, childUserId, academy, Set.copyOf(weekdays), classEndTime, targetPickup,
				statusOf(academy));
	}

	public static PickupSchedule restore(Long scheduleId, long childUserId, Academy academy, Set<DayOfWeek> weekdays,
			LocalTime classEndTime, int targetPickup, ScheduleStatus status) {
		return new PickupSchedule(scheduleId, childUserId, academy, Set.copyOf(weekdays), classEndTime, targetPickup,
				status);
	}

	public LocalTime targetPickupTime() {
		return classEndTime.plusMinutes(targetPickup);
	}

	private static ScheduleStatus statusOf(Academy academy) {
		return academy.hasEntrance() ? ScheduleStatus.READY : ScheduleStatus.DRAFT;
	}

	private static void validateWeekdays(Set<DayOfWeek> weekdays) {
		if (weekdays == null || weekdays.isEmpty()) {
			throw BusinessException.invalidField("weekdays", "1개 이상이어야 합니다");
		}
	}

	private static void validateTargetPickup(int targetPickup) {
		if (targetPickup < MIN_TARGET_PICKUP || targetPickup > MAX_TARGET_PICKUP) {
			throw BusinessException.invalidField("targetPickup", "0~60이어야 합니다");
		}
	}
}

package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.DailyPickups;
import com.example.pickupservice.schedule.application.port.in.PickupReadiness;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickup;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickups;
import com.example.pickupservice.schedule.domain.MissingSetupItem;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record WeeklyPickupsResponse(
		String weekStart,
		List<DayResponse> days,
		ReadinessResponse readiness
) {

	private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

	public static WeeklyPickupsResponse from(WeeklyPickups weeklyPickups) {
		return new WeeklyPickupsResponse(
				weeklyPickups.weekStart().format(YYYY_MM_DD),
				weeklyPickups.days().stream().map(DayResponse::from).toList(),
				ReadinessResponse.from(weeklyPickups.readiness()));
	}

	public record DayResponse(String date, List<PickupResponse> pickups) {

		static DayResponse from(DailyPickups day) {
			return new DayResponse(
					day.date().format(YYYY_MM_DD),
					day.pickups().stream().map(PickupResponse::from).toList());
		}
	}

	public record PickupResponse(
			long scheduleId,
			String childName,
			String academyName,
			String classEndTime,
			String targetPickupTime
	) {

		static PickupResponse from(WeeklyPickup pickup) {
			return new PickupResponse(
					pickup.scheduleId(),
					pickup.childName(),
					pickup.academyName(),
					pickup.classEndTime().format(HH_MM),
					pickup.targetPickupTime().format(HH_MM));
		}
	}

	public record ReadinessResponse(boolean startable, List<MissingSetupItem> missing) {

		static ReadinessResponse from(PickupReadiness readiness) {
			return new ReadinessResponse(readiness.startable(), readiness.missing());
		}
	}
}

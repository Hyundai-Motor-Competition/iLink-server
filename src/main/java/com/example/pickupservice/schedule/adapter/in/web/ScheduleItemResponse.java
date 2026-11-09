package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.ScheduleView;
import com.example.pickupservice.schedule.domain.ScheduleStatus;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ScheduleItemResponse(
		long scheduleId,
		Academy academy,
		List<String> weekdays,
		String classEndTime,
		int targetPickup,
		ScheduleStatus status
) {

	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

	public static ScheduleItemResponse from(ScheduleView view) {
		return new ScheduleItemResponse(
				view.scheduleId(),
				new Academy(view.academy().academyId(), view.academy().name()),
				view.weekdays().stream().map(ScheduleItemResponse::shortName).toList(),
				view.classEndTime().format(HH_MM),
				view.targetPickup(),
				view.status());
	}

	private static String shortName(DayOfWeek day) {
		return day.name().substring(0, 3);
	}

	public record Academy(
			long academyId,
			String name
	) {
	}
}

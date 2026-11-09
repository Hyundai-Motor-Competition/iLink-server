package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.CreateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ScheduleRequest(
		@NotNull @Valid AcademyRequest academy,
		@NotEmpty List<@NotNull @Pattern(regexp = "MON|TUE|WED|THU|FRI|SAT|SUN") String> weekdays,
		@NotNull @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d") String classEndTime,
		@NotNull @Min(0) @Max(60) Integer targetPickup
) {

	private static final Map<String, DayOfWeek> WEEKDAYS = Map.of(
			"MON", DayOfWeek.MONDAY,
			"TUE", DayOfWeek.TUESDAY,
			"WED", DayOfWeek.WEDNESDAY,
			"THU", DayOfWeek.THURSDAY,
			"FRI", DayOfWeek.FRIDAY,
			"SAT", DayOfWeek.SATURDAY,
			"SUN", DayOfWeek.SUNDAY);

	CreateScheduleCommand toCreateCommand(long familyId, long childId) {
		return new CreateScheduleCommand(familyId, childId, academy.toInput(), weekdaySet(),
				classEndLocalTime(), targetPickup);
	}

	UpdateScheduleCommand toUpdateCommand(long familyId, long scheduleId) {
		return new UpdateScheduleCommand(familyId, scheduleId, academy.toInput(), weekdaySet(),
				classEndLocalTime(), targetPickup);
	}

	private Set<DayOfWeek> weekdaySet() {
		Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
		weekdays.forEach(weekday -> days.add(WEEKDAYS.get(weekday)));
		return days;
	}

	private LocalTime classEndLocalTime() {
		return LocalTime.parse(classEndTime);
	}
}

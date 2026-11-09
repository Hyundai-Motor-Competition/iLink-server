package com.example.pickupservice.schedule.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record WeeklyPickups(
		LocalDate weekStart,
		List<DailyPickups> days,
		PickupReadiness readiness
) {
}

package com.example.pickupservice.schedule.application.port.in;

import java.time.LocalDate;
import java.util.List;

public record DailyPickups(
		LocalDate date,
		List<WeeklyPickup> pickups
) {
}

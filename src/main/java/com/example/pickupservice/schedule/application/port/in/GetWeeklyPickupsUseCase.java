package com.example.pickupservice.schedule.application.port.in;

import java.time.LocalDate;

public interface GetWeeklyPickupsUseCase {

	WeeklyPickups getWeeklyPickups(long familyId, LocalDate weekStart);
}

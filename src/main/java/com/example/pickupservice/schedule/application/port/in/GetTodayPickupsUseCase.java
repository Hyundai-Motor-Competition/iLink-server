package com.example.pickupservice.schedule.application.port.in;

import java.util.List;

public interface GetTodayPickupsUseCase {

	List<TodayPickup> getTodayPickups(long familyId);
}

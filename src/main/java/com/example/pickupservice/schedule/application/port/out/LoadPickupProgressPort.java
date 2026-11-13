package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.application.port.in.TodayPickupStatus;

import java.time.LocalDate;
import java.util.Map;

public interface LoadPickupProgressPort {

	Map<Long, TodayPickupStatus> loadProgress(long familyId, LocalDate date);
}

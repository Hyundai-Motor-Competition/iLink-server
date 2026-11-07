package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.domain.PickupSchedule;

import java.util.List;
import java.util.Optional;

public interface LoadSchedulePort {

	Optional<PickupSchedule> loadSchedule(long scheduleId);

	List<PickupSchedule> loadSchedulesOfChild(long childUserId);

	List<PickupSchedule> loadSchedulesOfChildren(List<Long> childUserIds);
}

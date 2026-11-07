package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.domain.PickupSchedule;

public interface SaveSchedulePort {

	PickupSchedule saveSchedule(PickupSchedule schedule);
}

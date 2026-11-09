package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;

import java.util.List;

public interface LoadSchedulePort {

	List<ScheduleInfo> loadSchedules(List<Long> scheduleIds);

}

package com.example.pickupservice.pickup.adapter.out.schedule;

import com.example.pickupservice.pickup.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.in.LoadScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class PickupScheduleAdapter implements LoadSchedulePort {

	private final LoadScheduleUseCase loadScheduleUseCase;

	@Override
	public List<ScheduleInfo> loadSchedules(List<Long> scheduleIds) {
		return loadScheduleUseCase.findSchedules(scheduleIds);
	}
}

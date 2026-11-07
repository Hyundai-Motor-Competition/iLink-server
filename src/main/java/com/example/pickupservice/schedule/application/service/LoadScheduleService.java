package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.in.LoadScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class LoadScheduleService implements LoadScheduleUseCase {

	private final LoadSchedulePort loadSchedulePort;

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleInfo> findSchedules(List<Long> scheduleIds) {
		if (scheduleIds.isEmpty()) {
			return List.of();
		}
		return scheduleIds.stream()
				.map(loadSchedulePort::loadSchedule)
				.flatMap(Optional::stream)
				.map(LoadScheduleService::toInfo)
				.toList();
	}

	private static ScheduleInfo toInfo(PickupSchedule schedule) {
		return new ScheduleInfo(schedule.getScheduleId(), schedule.getChildUserId(),
				AcademyInfoMapper.toInfo(schedule.getAcademy()), schedule.getWeekdays(), schedule.getClassEndTime(),
				schedule.getTargetPickup(), schedule.getStatus());
	}
}

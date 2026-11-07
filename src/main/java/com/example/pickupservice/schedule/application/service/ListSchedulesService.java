package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.ListSchedulesUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleAcademy;
import com.example.pickupservice.schedule.application.port.in.ScheduleView;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSchedulesService implements ListSchedulesUseCase {

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadSchedulePort loadSchedulePort;

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleView> listSchedules(ChildQuery query) {
		FamilyChildGuard.requireChildOfFamily(loadMemberUseCase, query.familyId(), query.childId());

		return loadSchedulePort.loadSchedulesOfChild(query.childId()).stream()
				.sorted(Comparator.comparing(PickupSchedule::getScheduleId))
				.map(ListSchedulesService::toView)
				.toList();
	}

	private static ScheduleView toView(PickupSchedule schedule) {
		Academy academy = schedule.getAcademy();
		return new ScheduleView(schedule.getScheduleId(),
				new ScheduleAcademy(academy.getAcademyId(), academy.getName()),
				schedule.getWeekdays().stream().sorted().toList(),
				schedule.getClassEndTime(), schedule.getTargetPickup(), schedule.getStatus());
	}
}

package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.in.CreateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SaveAcademyPort;
import com.example.pickupservice.schedule.application.port.out.SaveSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateScheduleService implements CreateScheduleUseCase {

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadSchedulePort loadSchedulePort;
	private final LoadAcademyPort loadAcademyPort;
	private final SaveAcademyPort saveAcademyPort;
	private final SaveSchedulePort saveSchedulePort;

	@Override
	@Transactional
	public ScheduleResult createSchedule(CreateScheduleCommand command) {
		FamilyChildGuard.requireChildOfFamily(loadMemberUseCase, command.familyId(), command.childId());

		Academy academy = ScheduleWriteRules.academyOf(loadAcademyPort, command.familyId(), command.academy());
		ScheduleWriteRules.rejectConflicts(scheduleOf(command, academy),
				loadSchedulePort.loadSchedulesOfChild(command.childId()));

		Academy savedAcademy = saveAcademyPort.saveAcademy(academy);
		PickupSchedule savedSchedule = saveSchedulePort.saveSchedule(scheduleOf(command, savedAcademy));
		return new ScheduleResult(savedSchedule.getScheduleId(), savedAcademy.getAcademyId(),
				savedSchedule.getStatus());
	}

	private static PickupSchedule scheduleOf(CreateScheduleCommand command, Academy academy) {
		return ScheduleWriteRules.newSchedule(command.childId(), academy, command.weekdays(), command.classEndTime(),
				command.targetPickup());
	}
}

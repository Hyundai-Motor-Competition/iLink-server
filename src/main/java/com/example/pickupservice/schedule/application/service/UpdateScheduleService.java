package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleUseCase;
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

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UpdateScheduleService implements UpdateScheduleUseCase {

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadSchedulePort loadSchedulePort;
	private final LoadAcademyPort loadAcademyPort;
	private final SaveAcademyPort saveAcademyPort;
	private final SaveSchedulePort saveSchedulePort;

	@Override
	@Transactional
	public ScheduleResult updateSchedule(UpdateScheduleCommand command) {
		PickupSchedule existing = loadSchedulePort.loadSchedule(command.scheduleId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		FamilyChildGuard.requireChildOfFamily(loadMemberUseCase, command.familyId(), existing.getChildUserId());

		Academy academy = ScheduleWriteRules.academyOf(loadAcademyPort, command.familyId(), command.academy());
		PickupSchedule candidate = ScheduleWriteRules.newSchedule(existing.getChildUserId(), academy, command.weekdays(),
				command.classEndTime(), command.targetPickup());
		ScheduleWriteRules.rejectConflicts(candidate, othersOf(existing));

		Academy savedAcademy = saveAcademyPort.saveAcademy(academy);
		PickupSchedule savedSchedule = saveSchedulePort.saveSchedule(replaced(existing, candidate, savedAcademy));
		ScheduleWriteRules.rejudgeDraftSchedulesOfAcademy(loadSchedulePort, saveSchedulePort, savedAcademy,
				savedSchedule.getScheduleId());
		return new ScheduleResult(savedSchedule.getScheduleId(), savedAcademy.getAcademyId(),
				savedSchedule.getStatus());
	}

	private List<PickupSchedule> othersOf(PickupSchedule existing) {
		return loadSchedulePort.loadSchedulesOfChild(existing.getChildUserId()).stream()
				.filter(schedule -> !Objects.equals(schedule.getScheduleId(), existing.getScheduleId()))
				.toList();
	}

	private static PickupSchedule replaced(PickupSchedule existing, PickupSchedule candidate, Academy savedAcademy) {
		PickupSchedule judged = ScheduleWriteRules.newSchedule(existing.getChildUserId(), savedAcademy,
				candidate.getWeekdays(), candidate.getClassEndTime(), candidate.getTargetPickup());
		return PickupSchedule.restore(existing.getScheduleId(), existing.getChildUserId(), savedAcademy,
				judged.getWeekdays(), judged.getClassEndTime(), judged.getTargetPickup(), judged.getStatus());
	}
}

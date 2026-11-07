package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SaveAcademyPort;
import com.example.pickupservice.schedule.application.port.out.SaveSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.DailyScheduleLimitRule;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class CreateScheduleService implements CreateScheduleUseCase {

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadSchedulePort loadSchedulePort;
	private final SaveAcademyPort saveAcademyPort;
	private final SaveSchedulePort saveSchedulePort;

	@Override
	@Transactional
	public ScheduleResult createSchedule(CreateScheduleCommand command) {
		requireChildOfFamily(command.familyId(), command.childId());

		AcademyInput input = command.academy();
		Academy academy = Academy.create(input.name(), input.address(), input.location(), input.entrance(),
				input.phone());
		PickupSchedule candidate = PickupSchedule.create(command.childId(), academy, command.weekdays(),
				command.classEndTime(), command.targetPickup());
		if (DailyScheduleLimitRule.exceedsLimit(candidate, loadSchedulePort.loadSchedulesOfChild(command.childId()))) {
			throw ScheduleErrors.dailyScheduleLimit();
		}

		Academy savedAcademy = saveAcademyPort.saveAcademy(academy);
		PickupSchedule savedSchedule = saveSchedulePort.saveSchedule(PickupSchedule.create(command.childId(),
				savedAcademy, command.weekdays(), command.classEndTime(), command.targetPickup()));

		return new ScheduleResult(savedSchedule.getScheduleId(), savedAcademy.getAcademyId(),
				savedSchedule.getStatus());
	}

	private void requireChildOfFamily(long familyId, long childId) {
		loadMemberUseCase.findMember(childId)
				.filter(member -> member.familyId() == familyId)
				.filter(member -> member.role() == Role.CHILD)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
	}
}

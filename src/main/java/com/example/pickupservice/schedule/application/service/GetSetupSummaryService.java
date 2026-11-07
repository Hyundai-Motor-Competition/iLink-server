package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.GetSetupSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.SetupChild;
import com.example.pickupservice.schedule.application.port.in.SetupSchedule;
import com.example.pickupservice.schedule.application.port.in.SetupSpot;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import com.example.pickupservice.schedule.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.LoadFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetSetupSummaryService implements GetSetupSummaryUseCase {

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadFamilyUseCase loadFamilyUseCase;
	private final LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;
	private final LoadSchedulePort loadSchedulePort;
	private final LoadPickupSpotPort loadPickupSpotPort;

	@Override
	@Transactional(readOnly = true)
	public SetupSummary getSetupSummary(ChildQuery query) {
		MemberInfo child = requireChildOfFamily(query.familyId(), query.childId());
		FamilyInfo family = loadFamilyUseCase.findFamily(query.familyId());

		DeviceStatus deviceStatus = loadMemberDeviceStatusPort.loadDeviceStatuses(List.of(query.childId()))
				.getOrDefault(query.childId(), DeviceStatus.NONE);

		List<PickupSchedule> schedules = loadSchedulePort.loadSchedulesOfChild(query.childId());
		List<Long> academyIds = schedules.stream()
				.map(schedule -> schedule.getAcademy().getAcademyId())
				.distinct()
				.toList();
		List<SpotSummary> spots = academyIds.isEmpty()
				? List.of()
				: loadPickupSpotPort.loadSpots(query.familyId(), academyIds);

		List<MissingSetupItem> missing = new ArrayList<>();
		if (deviceStatus != DeviceStatus.CONNECTED) {
			missing.add(MissingSetupItem.CHILD_DEVICE);
		}
		if (schedules.stream().noneMatch(schedule -> schedule.getStatus() == ScheduleStatus.READY)) {
			missing.add(MissingSetupItem.SCHEDULE);
		}
		Set<Long> academiesWithDefaultSpot = spots.stream()
				.filter(SpotSummary::isDefault)
				.map(SpotSummary::academyId)
				.collect(Collectors.toSet());
		if (!academiesWithDefaultSpot.containsAll(academyIds)) {
			missing.add(MissingSetupItem.PICKUP_SPOT);
		}

		return new SetupSummary(
				new SetupChild(child.userId(), child.name(), deviceStatus),
				schedules.stream()
						.map(schedule -> new SetupSchedule(schedule.getScheduleId(), schedule.getAcademy().getName(),
								schedule.getStatus()))
						.toList(),
				spots.stream()
						.map(spot -> new SetupSpot(spot.spotId(), spot.name(), spot.isDefault()))
						.toList(),
				family.joinCallMode(),
				List.copyOf(missing),
				family.settingsVersion());
	}

	private MemberInfo requireChildOfFamily(long familyId, long childId) {
		return loadMemberUseCase.findMember(childId)
				.filter(member -> member.familyId() == familyId)
				.filter(member -> member.role() == Role.CHILD)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
	}
}

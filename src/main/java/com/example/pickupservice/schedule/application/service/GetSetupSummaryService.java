package com.example.pickupservice.schedule.application.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
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
		MemberInfo child = FamilyChildGuard.requireChildOfFamily(loadMemberUseCase, query.familyId(),
				query.childId());
		FamilyInfo family = loadFamilyUseCase.findFamily(query.familyId());
		DeviceStatus deviceStatus = deviceStatusOf(query.childId());
		List<PickupSchedule> schedules = loadSchedulePort.loadSchedulesOfChild(query.childId());
		List<SpotSummary> spots = spotsOf(query.familyId(), SetupChecks.academyIdsOf(schedules));

		return new SetupSummary(new SetupChild(child.userId(), child.name(), deviceStatus),
				toSetupSchedules(schedules), toSetupSpots(spots), family.joinCallMode(),
				missingItems(deviceStatus, schedules, spots), family.settingsVersion());
	}

	private DeviceStatus deviceStatusOf(long childId) {
		return loadMemberDeviceStatusPort.loadDeviceStatuses(List.of(childId))
				.getOrDefault(childId, DeviceStatus.NONE);
	}

	private List<SpotSummary> spotsOf(long familyId, List<Long> academyIds) {
		return academyIds.isEmpty() ? List.of() : loadPickupSpotPort.loadSpots(familyId, academyIds);
	}

	private static List<MissingSetupItem> missingItems(DeviceStatus deviceStatus, List<PickupSchedule> schedules,
			List<SpotSummary> spots) {
		List<MissingSetupItem> missing = new ArrayList<>();
		if (deviceStatus != DeviceStatus.CONNECTED) {
			missing.add(MissingSetupItem.CHILD_DEVICE);
		}
		if (schedules.stream().noneMatch(schedule -> schedule.getStatus() == ScheduleStatus.READY)) {
			missing.add(MissingSetupItem.SCHEDULE);
		}
		if (SetupChecks.lacksDefaultSpot(spots, SetupChecks.academyIdsOf(schedules))) {
			missing.add(MissingSetupItem.PICKUP_SPOT);
		}
		return List.copyOf(missing);
	}

	private static List<SetupSchedule> toSetupSchedules(List<PickupSchedule> schedules) {
		return schedules.stream()
				.map(schedule -> new SetupSchedule(schedule.getScheduleId(), schedule.getAcademy().getName(),
						schedule.getStatus()))
				.toList();
	}

	private static List<SetupSpot> toSetupSpots(List<SpotSummary> spots) {
		return spots.stream()
				.map(spot -> new SetupSpot(spot.spotId(), spot.name(), spot.isDefault()))
				.toList();
	}
}

package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.schedule.application.port.in.DailyPickups;
import com.example.pickupservice.schedule.application.port.in.GetWeeklyPickupsUseCase;
import com.example.pickupservice.schedule.application.port.in.PickupReadiness;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickup;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickups;
import com.example.pickupservice.schedule.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GetWeeklyPickupsService implements GetWeeklyPickupsUseCase {

	private static final int DAYS_OF_WEEK = 7;

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadSchedulePort loadSchedulePort;
	private final LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;
	private final LoadPickupSpotPort loadPickupSpotPort;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public WeeklyPickups getWeeklyPickups(long familyId, LocalDate weekStart) {
		LocalDate today = TimeUtils.todayKst(clock);
		LocalDate start = weekStart != null
				? weekStart
				: today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<MemberInfo> children = loadMemberUseCase.findMembers(familyId).stream()
				.filter(member -> member.role() == Role.CHILD)
				.toList();
		Map<Long, String> childNames = new HashMap<>();
		Map<Long, String> profileImageUrls = new HashMap<>();
		for (MemberInfo child : children) {
			childNames.put(child.userId(), child.name());
			profileImageUrls.put(child.userId(), child.profileImageUrl());
		}
		List<Long> childIds = List.copyOf(childNames.keySet());
		List<PickupSchedule> schedules = loadSchedulePort.loadSchedulesOfChildren(childIds);

		List<DailyPickups> days = IntStream.range(0, DAYS_OF_WEEK)
				.mapToObj(offset -> dailyPickups(start.plusDays(offset), schedules, childNames, profileImageUrls))
				.toList();
		return new WeeklyPickups(start, days, readiness(familyId, schedulesOn(schedules, today.getDayOfWeek())));
	}

	private static DailyPickups dailyPickups(LocalDate date, List<PickupSchedule> schedules,
			Map<Long, String> childNames, Map<Long, String> profileImageUrls) {
		List<WeeklyPickup> pickups = schedulesOn(schedules, date.getDayOfWeek()).stream()
				.map(schedule -> new WeeklyPickup(schedule.getScheduleId(),
						childNames.get(schedule.getChildUserId()), profileImageUrls.get(schedule.getChildUserId()),
						schedule.getAcademy().getName(),
						schedule.getClassEndTime(), schedule.targetPickupTime()))
				.toList();
		return new DailyPickups(date, pickups);
	}

	private static List<PickupSchedule> schedulesOn(List<PickupSchedule> schedules, DayOfWeek dayOfWeek) {
		return schedules.stream()
				.filter(schedule -> schedule.getWeekdays().contains(dayOfWeek))
				.sorted(Comparator.comparing(PickupSchedule::getClassEndTime))
				.toList();
	}

	private PickupReadiness readiness(long familyId, List<PickupSchedule> todaySchedules) {
		if (todaySchedules.isEmpty()) {
			return new PickupReadiness(false, List.of());
		}
		List<MissingSetupItem> missing = new ArrayList<>();
		if (!allDevicesConnected(todaySchedules)) {
			missing.add(MissingSetupItem.CHILD_DEVICE);
		}
		if (hasDraft(todaySchedules)) {
			missing.add(MissingSetupItem.SCHEDULE);
		}
		List<Long> academyIds = SetupChecks.academyIdsOf(todaySchedules);
		if (SetupChecks.lacksDefaultSpot(loadPickupSpotPort.loadSpots(familyId, academyIds), academyIds)) {
			missing.add(MissingSetupItem.PICKUP_SPOT);
		}
		return new PickupReadiness(missing.isEmpty(), List.copyOf(missing));
	}

	private static boolean hasDraft(List<PickupSchedule> todaySchedules) {
		return todaySchedules.stream().anyMatch(schedule -> schedule.getStatus() == ScheduleStatus.DRAFT);
	}

	private boolean allDevicesConnected(List<PickupSchedule> todaySchedules) {
		List<Long> childIds = todaySchedules.stream()
				.map(PickupSchedule::getChildUserId)
				.distinct()
				.toList();
		Map<Long, DeviceStatus> deviceStatuses = loadMemberDeviceStatusPort.loadDeviceStatuses(childIds);
		return childIds.stream()
				.allMatch(childId -> deviceStatuses.getOrDefault(childId, DeviceStatus.NONE) == DeviceStatus.CONNECTED);
	}
}

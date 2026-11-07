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
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

		Map<Long, String> childNames = loadMemberUseCase.findMembers(familyId).stream()
				.filter(member -> member.role() == Role.CHILD)
				.collect(Collectors.toMap(MemberInfo::userId, MemberInfo::name));
		List<PickupSchedule> schedules = loadSchedulePort.loadSchedulesOfChildren(List.copyOf(childNames.keySet()));

		List<DailyPickups> days = new ArrayList<>();
		for (int i = 0; i < DAYS_OF_WEEK; i++) {
			LocalDate date = start.plusDays(i);
			List<WeeklyPickup> pickups = schedulesOn(schedules, date.getDayOfWeek()).stream()
					.map(schedule -> new WeeklyPickup(schedule.getScheduleId(),
							childNames.get(schedule.getChildUserId()), schedule.getAcademy().getName(),
							schedule.getClassEndTime(), schedule.targetPickupTime()))
					.toList();
			days.add(new DailyPickups(date, pickups));
		}

		return new WeeklyPickups(start, List.copyOf(days),
				readiness(familyId, schedulesOn(schedules, today.getDayOfWeek())));
	}

	private List<PickupSchedule> schedulesOn(List<PickupSchedule> schedules, DayOfWeek dayOfWeek) {
		return schedules.stream()
				.filter(schedule -> schedule.getWeekdays().contains(dayOfWeek))
				.sorted(Comparator.comparing(PickupSchedule::getClassEndTime))
				.toList();
	}

	private PickupReadiness readiness(long familyId, List<PickupSchedule> todaySchedules) {
		if (todaySchedules.isEmpty()) {
			return new PickupReadiness(false, List.of());
		}

		List<Long> childIds = todaySchedules.stream()
				.map(PickupSchedule::getChildUserId)
				.distinct()
				.toList();
		List<Long> academyIds = todaySchedules.stream()
				.map(schedule -> schedule.getAcademy().getAcademyId())
				.distinct()
				.toList();

		List<MissingSetupItem> missing = new ArrayList<>();
		Map<Long, DeviceStatus> deviceStatuses = loadMemberDeviceStatusPort.loadDeviceStatuses(childIds);
		boolean allConnected = childIds.stream()
				.allMatch(childId -> deviceStatuses.getOrDefault(childId, DeviceStatus.NONE) == DeviceStatus.CONNECTED);
		if (!allConnected) {
			missing.add(MissingSetupItem.CHILD_DEVICE);
		}

		Set<Long> academiesWithDefaultSpot = loadPickupSpotPort.loadSpots(familyId, academyIds).stream()
				.filter(SpotSummary::isDefault)
				.map(SpotSummary::academyId)
				.collect(Collectors.toSet());
		if (!academiesWithDefaultSpot.containsAll(academyIds)) {
			missing.add(MissingSetupItem.PICKUP_SPOT);
		}

		return new PickupReadiness(missing.isEmpty(), List.copyOf(missing));
	}
}

package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.schedule.application.port.in.GetTodayPickupsUseCase;
import com.example.pickupservice.schedule.application.port.in.TodayPickup;
import com.example.pickupservice.schedule.application.port.in.TodayPickupStatus;
import com.example.pickupservice.schedule.application.port.out.LoadPickupProgressPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetTodayPickupsService implements GetTodayPickupsUseCase {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadSchedulePort loadSchedulePort;
	private final LoadPickupProgressPort loadPickupProgressPort;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<TodayPickup> getTodayPickups(long familyId) {
		LocalDate today = TimeUtils.todayKst(clock);
		Map<Long, String> childNames = loadMemberUseCase.findMembers(familyId).stream()
				.filter(member -> member.role() == Role.CHILD)
				.collect(Collectors.toMap(MemberInfo::userId, MemberInfo::name));
		List<PickupSchedule> todaySchedules = loadSchedulePort.loadSchedulesOfChildren(
						List.copyOf(childNames.keySet())).stream()
				.filter(schedule -> schedule.getWeekdays().contains(today.getDayOfWeek()))
				.toList();
		if (todaySchedules.isEmpty()) {
			return List.of();
		}
		Map<Long, TodayPickupStatus> progress = loadPickupProgressPort.loadProgress(familyId, today);

		return todaySchedules.stream()
				.map(schedule -> new TodayPickup(schedule.getScheduleId(), childNames.get(schedule.getChildUserId()),
						schedule.getAcademy().getName(),
						today.atTime(schedule.targetPickupTime()).atZone(SEOUL).toInstant(),
						progress.getOrDefault(schedule.getScheduleId(), TodayPickupStatus.SCHEDULED)))
				.sorted(Comparator.comparing(TodayPickup::targetAt).thenComparingLong(TodayPickup::scheduleId))
				.toList();
	}
}

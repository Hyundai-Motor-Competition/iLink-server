package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.schedule.application.port.in.GetNextPickupUseCase;
import com.example.pickupservice.schedule.application.port.in.NextPickup;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
public class GetNextPickupService implements GetNextPickupUseCase {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final int SEARCH_DAYS = 7;

	private final LoadSchedulePort loadSchedulePort;
	private final LoadPickupSpotPort loadPickupSpotPort;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public Optional<NextPickup> getNextPickup(long familyId, long childUserId) {
		Instant now = clock.instant();
		LocalDate today = TimeUtils.todayKst(clock);
		List<PickupSchedule> readySchedules = loadSchedulePort.loadSchedulesOfChild(childUserId).stream()
				.filter(schedule -> schedule.getStatus() == ScheduleStatus.READY)
				.toList();

		return LongStream.rangeClosed(0, SEARCH_DAYS)
				.mapToObj(today::plusDays)
				.flatMap(date -> readySchedules.stream()
						.filter(schedule -> schedule.getWeekdays().contains(date.getDayOfWeek()))
						.map(schedule -> new Candidate(schedule, targetAt(date, schedule))))
				.filter(candidate -> candidate.targetAt().isAfter(now))
				.min(Comparator.comparing(Candidate::targetAt))
				.map(candidate -> toNextPickup(familyId, candidate));
	}

	private static Instant targetAt(LocalDate date, PickupSchedule schedule) {
		return date.atTime(schedule.targetPickupTime()).atZone(SEOUL).toInstant();
	}

	private NextPickup toNextPickup(long familyId, Candidate candidate) {
		long academyId = candidate.schedule().getAcademy().getAcademyId();
		String spotName = loadPickupSpotPort.loadSpots(familyId, List.of(academyId)).stream()
				.filter(spot -> spot.academyId() == academyId && spot.isDefault())
				.map(SpotSummary::name)
				.findFirst()
				.orElse(null);
		return new NextPickup(candidate.schedule().getAcademy().getName(), spotName, candidate.targetAt());
	}

	private record Candidate(PickupSchedule schedule, Instant targetAt) {
	}
}

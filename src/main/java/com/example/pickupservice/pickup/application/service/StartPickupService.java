package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.in.StartPickupCommand;
import com.example.pickupservice.pickup.application.port.in.StartPickupResult;
import com.example.pickupservice.pickup.application.port.in.StartPickupUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadChildReadinessPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSchedulePort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.domain.ChildSchedule;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SpotSnapshot;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StartPickupService implements StartPickupUseCase {

	private static final String MISSING_CHILD_DEVICE = "CHILD_DEVICE";
	private static final String MISSING_SCHEDULE = "SCHEDULE";
	private static final String MISSING_PICKUP_SPOT = "PICKUP_SPOT";
	private static final String SESSION_STARTED = SessionEventType.SESSION_STARTED.name();

	private final LoadSchedulePort loadSchedulePort;
	private final LoadSpotPort loadSpotPort;
	private final LoadChildReadinessPort loadChildReadinessPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadCallPort loadCallPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final RecordNotificationPort recordNotificationPort;
	private final SessionViewAssembler sessionViewAssembler;
	private final Clock clock;

	@Override
	public StartPickupResult start(StartPickupCommand command) {
		if (!TimeUtils.todayKst(clock).equals(command.date())) {
			throw PickupErrors.notToday();
		}
		List<ScheduleInfo> schedules = loadSchedulePort.loadSchedules(command.scheduleIds());
		long familyId = command.principal().familyId();
		requireAllFoundInFamily(schedules, command, familyId);
		requireOnePerChildOnToday(schedules, command.date());
		long academyId = singleAcademyId(schedules);
		requireSameTargetPickupTime(schedules);
		Optional<SpotInfo> defaultSpot = loadSpotPort.loadDefaultSpot(familyId, academyId);
		requireSetupComplete(schedules, defaultSpot);
		Optional<PickupSession> activeSession = loadSessionPort.loadActiveByVehicle(command.principal().id());
		if (activeSession.isPresent()) {
			return sameSessionOrReject(activeSession.get(), command);
		}
		PickupSession session = saveSessionPort.save(PickupSession.start(command.principal().id(), familyId,
				command.date(), snapshotOf(defaultSpot.orElseThrow()), childSchedulesOf(schedules)));
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		List<Long> receiverUserIds = SessionReceivers.guardiansAndSessionChildren(session, members);
		Instant now = clock.instant();
		SessionEvent event = sessionEventPort.append(session.getSessionId(), SESSION_STARTED,
				session.getStateVersion(), now, Map.of());
		List<Long> notificationIds = recordNotificationPort.recordNotifications(
				new NotificationRequest(session.getSessionId(), null, SESSION_STARTED, receiverUserIds));
		publishSessionEventPort.publish(SessionEventMessages.of(event, receiverUserIds, notificationIds));
		return new StartPickupResult(true, sessionViewAssembler.assemble(session, command.principal(),
				loadFamilyPort.loadFamily(familyId), members, loadCallPort.loadLatestCall(session.getSessionId()), now));
	}

	private void requireAllFoundInFamily(List<ScheduleInfo> schedules, StartPickupCommand command, long familyId) {
		if (schedules.size() < Set.copyOf(command.scheduleIds()).size()) {
			throw PickupErrors.notFound();
		}
		boolean allInFamily = schedules.stream()
				.map(ScheduleInfo::childUserId)
				.distinct()
				.allMatch(childUserId -> loadFamilyPort.loadMember(childUserId)
						.filter(member -> member.familyId() == familyId)
						.isPresent());
		if (!allInFamily) {
			throw PickupErrors.notFound();
		}
	}

	private static void requireOnePerChildOnToday(List<ScheduleInfo> schedules, LocalDate date) {
		boolean allToday = schedules.stream().allMatch(schedule -> schedule.weekdays().contains(date.getDayOfWeek()));
		long distinctChildren = schedules.stream().map(ScheduleInfo::childUserId).distinct().count();
		if (!allToday || distinctChildren != schedules.size()) {
			throw PickupErrors.schedulesNotGroupable();
		}
	}

	private static SpotSnapshot snapshotOf(SpotInfo spot) {
		return new SpotSnapshot(spot.spotId(), spot.name(), spot.stopPoint(), spot.meetingPoint());
	}

	private static List<ChildSchedule> childSchedulesOf(List<ScheduleInfo> schedules) {
		return schedules.stream()
				.map(schedule -> new ChildSchedule(schedule.childUserId(), schedule.scheduleId()))
				.toList();
	}

	private void requireSetupComplete(List<ScheduleInfo> schedules, Optional<SpotInfo> defaultSpot) {
		List<String> missing = new ArrayList<>();
		if (hasChildWithoutConnectedDevice(schedules)) {
			missing.add(MISSING_CHILD_DEVICE);
		}
		if (schedules.stream().anyMatch(schedule -> schedule.status() == ScheduleStatus.DRAFT)) {
			missing.add(MISSING_SCHEDULE);
		}
		if (defaultSpot.isEmpty()) {
			missing.add(MISSING_PICKUP_SPOT);
		}
		if (!missing.isEmpty()) {
			throw PickupErrors.setupIncomplete(missing);
		}
	}

	private boolean hasChildWithoutConnectedDevice(List<ScheduleInfo> schedules) {
		List<Long> childUserIds = schedules.stream()
				.map(ScheduleInfo::childUserId)
				.distinct()
				.toList();
		Map<Long, MemberDeviceStatus> deviceStatuses = loadChildReadinessPort.loadDeviceStatuses(childUserIds);
		return childUserIds.stream()
				.anyMatch(childUserId -> deviceStatuses.get(childUserId) != MemberDeviceStatus.CONNECTED);
	}

	private StartPickupResult sameSessionOrReject(PickupSession activeSession, StartPickupCommand command) {
		Set<Long> sessionScheduleIds = activeSession.getChildren().stream()
				.map(SessionChild::getScheduleId)
				.collect(Collectors.toSet());
		boolean sameSession = activeSession.getDate().equals(command.date())
				&& sessionScheduleIds.equals(Set.copyOf(command.scheduleIds()));
		if (!sameSession) {
			throw PickupErrors.anotherSessionActive();
		}
		return new StartPickupResult(false, viewOf(activeSession, command.principal()));
	}

	private SessionView viewOf(PickupSession session, AuthPrincipal viewer) {
		long familyId = session.getFamilyId();
		return sessionViewAssembler.assemble(session, viewer, loadFamilyPort.loadFamily(familyId),
				loadFamilyPort.loadMembers(familyId), loadCallPort.loadLatestCall(session.getSessionId()),
				clock.instant());
	}

	private long singleAcademyId(List<ScheduleInfo> schedules) {
		Set<Long> academyIds = schedules.stream()
				.map(schedule -> schedule.academy().academyId())
				.collect(Collectors.toSet());
		if (academyIds.size() != 1) {
			throw PickupErrors.schedulesNotGroupable();
		}
		return academyIds.iterator().next();
	}

	private void requireSameTargetPickupTime(List<ScheduleInfo> schedules) {
		long targetPickupTimes = schedules.stream()
				.map(schedule -> schedule.classEndTime().plusMinutes(schedule.targetPickup()))
				.distinct()
				.count();
		if (targetPickupTimes != 1) {
			throw PickupErrors.schedulesNotGroupable();
		}
	}

}

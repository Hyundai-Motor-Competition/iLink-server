package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.SessionChildView;
import com.example.pickupservice.pickup.application.port.in.StartPickupCommand;
import com.example.pickupservice.pickup.application.port.in.StartPickupResult;
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
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.schedule.application.port.in.AcademyInfo;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StartPickupServiceTest {

	// 한국 시간 2026-11-09(월) 17:00
	private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long ACADEMY_ID = 55L;
	private static final long OTHER_ACADEMY_ID = 56L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long SPOT_ID = 21L;
	private static final long OTHER_ACADEMY_SPOT_ID = 22L;
	private static final long OTHER_SCHEDULE_ID = 901L;
	private static final long EXISTING_SESSION_ID = 501L;
	private static final long NEW_SESSION_ID = 601L;
	private static final long NEW_EVENT_ID = 9001L;
	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LocalTime CLASS_END_TIME = LocalTime.of(17, 50);
	private static final int TARGET_PICKUP = 10;
	private static final Set<DayOfWeek> WEEKDAYS = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

	private static final AcademyInfo ACADEMY = new AcademyInfo(ACADEMY_ID, "해솔영어학원", "서울 강남구 테헤란로 1",
			new LatLng(37.501234, 127.039876), new LatLng(37.501300, 127.039900), "02-555-0101");
	private static final AcademyInfo OTHER_ACADEMY = new AcademyInfo(OTHER_ACADEMY_ID, "다온수학학원",
			"서울 강남구 테헤란로 9", new LatLng(37.502000, 127.041000), new LatLng(37.502050, 127.041050),
			"02-555-0202");

	@Mock
	private LoadSchedulePort loadSchedulePort;

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private LoadChildReadinessPort loadChildReadinessPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	@Mock
	private RecordNotificationPort recordNotificationPort;

	private StartPickupService startPickupService;

	@BeforeEach
	void setUp() {
		startPickupService = newService(Clock.fixed(NOW, ZoneOffset.UTC));
		givenSchedules(
				schedule(MINJUN_SCHEDULE_ID, MINJUN_ID, ACADEMY, CLASS_END_TIME, TARGET_PICKUP),
				schedule(SEOYUN_SCHEDULE_ID, SEOYUN_ID, ACADEMY, CLASS_END_TIME, TARGET_PICKUP));
		givenDefaultSpot(ACADEMY_ID, Optional.of(spot(SPOT_ID, ACADEMY_ID, "정문 앞")));
		givenDefaultSpot(OTHER_ACADEMY_ID, Optional.of(spot(OTHER_ACADEMY_SPOT_ID, OTHER_ACADEMY_ID, "후문")));
		lenient().when(loadChildReadinessPort.loadDeviceStatuses(anyList())).thenReturn(Map.of(
				MINJUN_ID, MemberDeviceStatus.CONNECTED,
				SEOYUN_ID, MemberDeviceStatus.CONNECTED));
		lenient().when(loadChildReadinessPort.hasLocationConsent(anyLong())).thenReturn(true);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(loadSessionPort.loadActiveByVehicle(VEHICLE_ID)).thenReturn(Optional.empty());
		lenient().when(loadSessionPort.loadActiveByFamily(FAMILY_ID)).thenReturn(Optional.empty());
	}

	@Test
	@DisplayName("픽업 시작은 date가 한국 날짜로 내일이면 NOT_TODAY로 거절한다")
	void startRejectsTomorrowKstDateWithNotToday() {
		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST.plusDays(1))));

		assertRejected(exception, "NOT_TODAY");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 date가 한국 날짜로 어제면 NOT_TODAY로 거절한다")
	void startRejectsYesterdayKstDateWithNotToday() {
		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST.minusDays(1))));

		assertRejected(exception, "NOT_TODAY");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 UTC 날짜로는 오늘이어도 한국 날짜로 이미 다음 날이면 NOT_TODAY로 거절한다")
	void startRejectsUtcTodayWithNotTodayWhenKstDateHasAlreadyChanged() {
		// UTC 2026-11-09 15:30 = 한국 2026-11-10 00:30
		StartPickupService afterKstMidnight = newService(
				Clock.fixed(Instant.parse("2026-11-09T15:30:00Z"), ZoneOffset.UTC));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> afterKstMidnight.start(command(LocalDate.of(2026, 11, 9))));

		assertRejected(exception, "NOT_TODAY");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 학원이 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다")
	void startRejectsSchedulesOfDifferentAcademiesWithSchedulesNotGroupable() {
		givenSchedules(
				schedule(MINJUN_SCHEDULE_ID, MINJUN_ID, ACADEMY, CLASS_END_TIME, TARGET_PICKUP),
				schedule(SEOYUN_SCHEDULE_ID, SEOYUN_ID, OTHER_ACADEMY, CLASS_END_TIME, TARGET_PICKUP));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SCHEDULES_NOT_GROUPABLE");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 수업 종료 시각이 달라 시간대가 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다")
	void startRejectsSchedulesWithDifferentClassEndTimeWithSchedulesNotGroupable() {
		// 17:50 + 10분 = 18:00, 18:50 + 10분 = 19:00
		givenSchedules(
				schedule(MINJUN_SCHEDULE_ID, MINJUN_ID, ACADEMY, LocalTime.of(17, 50), 10),
				schedule(SEOYUN_SCHEDULE_ID, SEOYUN_ID, ACADEMY, LocalTime.of(18, 50), 10));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SCHEDULES_NOT_GROUPABLE");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 목표 픽업 시각이 달라 시간대가 다른 일정이 섞이면 SCHEDULES_NOT_GROUPABLE로 거절한다")
	void startRejectsSchedulesWithDifferentTargetPickupWithSchedulesNotGroupable() {
		// 17:50 + 0분 = 17:50, 17:50 + 20분 = 18:10
		givenSchedules(
				schedule(MINJUN_SCHEDULE_ID, MINJUN_ID, ACADEMY, LocalTime.of(17, 50), 0),
				schedule(SEOYUN_SCHEDULE_ID, SEOYUN_ID, ACADEMY, LocalTime.of(17, 50), 20));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SCHEDULES_NOT_GROUPABLE");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 기본 장소가 없으면 SETUP_INCOMPLETE와 missing PICKUP_SPOT으로 거절한다")
	void startRejectsMissingDefaultSpotWithSetupIncompleteAndMissingPickupSpot() {
		givenDefaultSpot(ACADEMY_ID, Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SETUP_INCOMPLETE");
		assertThat(exception.details()).containsEntry("missing", List.of("PICKUP_SPOT"));
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 자녀 기기가 CONNECTED가 아니면 SETUP_INCOMPLETE와 missing CHILD_DEVICE로 거절한다")
	void startRejectsNotConnectedChildDeviceWithSetupIncompleteAndMissingChildDevice() {
		givenDeviceStatuses(Map.of(
				MINJUN_ID, MemberDeviceStatus.INVITE_PENDING,
				SEOYUN_ID, MemberDeviceStatus.CONNECTED));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SETUP_INCOMPLETE");
		assertThat(exception.details()).containsEntry("missing", List.of("CHILD_DEVICE"));
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 자녀 기기 상태가 아예 없는 자녀가 있으면 SETUP_INCOMPLETE와 missing CHILD_DEVICE로 거절한다")
	void startRejectsChildWithoutDeviceStatusWithSetupIncompleteAndMissingChildDevice() {
		// 서윤의 기기 상태가 없다 = 기기 없음
		givenDeviceStatuses(Map.of(MINJUN_ID, MemberDeviceStatus.CONNECTED));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SETUP_INCOMPLETE");
		assertThat(exception.details()).containsEntry("missing", List.of("CHILD_DEVICE"));
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 자녀 기기와 기본 장소가 모두 없으면 missing에 CHILD_DEVICE · PICKUP_SPOT을 이 순서로 담아 거절한다")
	void startRejectsMissingChildDeviceAndDefaultSpotWithBothInMissing() {
		givenDefaultSpot(ACADEMY_ID, Optional.empty());
		givenDeviceStatuses(Map.of(
				MINJUN_ID, MemberDeviceStatus.NONE,
				SEOYUN_ID, MemberDeviceStatus.CONNECTED));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SETUP_INCOMPLETE");
		assertThat(exception.details()).containsEntry("missing", List.of("CHILD_DEVICE", "PICKUP_SPOT"));
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 DRAFT 일정이 하나라도 있으면 SETUP_INCOMPLETE와 missing SCHEDULE로 거절한다")
	void startRejectsDraftScheduleWithSetupIncompleteAndMissingSchedule() {
		givenSchedules(
				schedule(MINJUN_SCHEDULE_ID, MINJUN_ID, ACADEMY, CLASS_END_TIME, TARGET_PICKUP, ScheduleStatus.DRAFT),
				schedule(SEOYUN_SCHEDULE_ID, SEOYUN_ID, ACADEMY, CLASS_END_TIME, TARGET_PICKUP));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, "SETUP_INCOMPLETE");
		assertThat(exception.details()).containsEntry("missing", List.of("SCHEDULE"));
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 그 차량에 다른 일정의 진행 중 세션이 있으면 ANOTHER_SESSION_ACTIVE로 거절한다")
	void startRejectsActiveSessionOfOtherSchedulesWithAnotherSessionActive() {
		givenActiveSessionOfVehicle(activeSession(EXISTING_SESSION_ID, 3, TODAY_KST,
				waitingChild(91L, MINJUN_ID, OTHER_SCHEDULE_ID)));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, HttpStatus.CONFLICT, "ANOTHER_SESSION_ACTIVE");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 그 차량의 진행 중 세션이 요청 일정 중 일부만 담고 있으면 ANOTHER_SESSION_ACTIVE로 거절한다")
	void startRejectsActiveSessionWithOnlyPartOfSchedulesWithAnotherSessionActive() {
		givenActiveSessionOfVehicle(activeSession(EXISTING_SESSION_ID, 3, TODAY_KST,
				waitingChild(91L, MINJUN_ID, MINJUN_SCHEDULE_ID)));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, HttpStatus.CONFLICT, "ANOTHER_SESSION_ACTIVE");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 그 차량의 진행 중 세션이 같은 일정이어도 date가 다르면 ANOTHER_SESSION_ACTIVE로 거절한다")
	void startRejectsActiveSessionOfOtherDateWithAnotherSessionActive() {
		givenActiveSessionOfVehicle(activeSession(EXISTING_SESSION_ID, 3, TODAY_KST.minusDays(1),
				waitingChild(91L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				waitingChild(92L, SEOYUN_ID, SEOYUN_SCHEDULE_ID)));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> startPickupService.start(command(TODAY_KST)));

		assertRejected(exception, HttpStatus.CONFLICT, "ANOTHER_SESSION_ACTIVE");
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 같은 차량 · 같은 date · 같은 일정 묶음의 진행 중 세션이 있으면 새로 만들지 않고 그 세션을 created false로 돌려준다")
	void startReturnsExistingSameSessionWithCreatedFalse() {
		givenActiveSessionOfVehicle(activeSession(EXISTING_SESSION_ID, 3, TODAY_KST,
				waitingChild(91L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				waitingChild(92L, SEOYUN_ID, SEOYUN_SCHEDULE_ID)));

		StartPickupResult result = startPickupService.start(command(TODAY_KST));

		assertThat(result.created()).isFalse();
		assertThat(result.session()).isNotNull();
		assertThat(result.session().sessionId()).isEqualTo(EXISTING_SESSION_ID);
		assertThat(result.session().stateVersion()).isEqualTo(3);
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 scheduleIds 순서만 다르고 같은 일정 묶음이면 그 진행 중 세션을 created false로 돌려준다")
	void startReturnsExistingSameSessionWithCreatedFalseWhenScheduleIdsOrderDiffers() {
		givenActiveSessionOfVehicle(activeSession(EXISTING_SESSION_ID, 3, TODAY_KST,
				waitingChild(91L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				waitingChild(92L, SEOYUN_ID, SEOYUN_SCHEDULE_ID)));

		StartPickupResult result = startPickupService.start(
				command(TODAY_KST, List.of(SEOYUN_SCHEDULE_ID, MINJUN_SCHEDULE_ID)));

		assertThat(result.created()).isFalse();
		assertThat(result.session()).isNotNull();
		assertThat(result.session().sessionId()).isEqualTo(EXISTING_SESSION_ID);
		assertThat(result.session().stateVersion()).isEqualTo(3);
		assertNothingStarted();
	}

	@Test
	@DisplayName("픽업 시작은 새 세션을 ACTIVE · ROUTING · SELECTED, 기본 장소 스냅샷, 자녀마다 WAITING 자녀로 저장한다")
	void startSavesNewSessionWithDefaultSpotSnapshotAndWaitingChildren() {
		givenNewSessionSaved();
		givenEventAppended();

		startPickupService.start(command(TODAY_KST));

		PickupSession saved = savedSession();
		assertThat(saved.getVehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getDate()).isEqualTo(TODAY_KST);
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.ROUTING);
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.SELECTED);
		assertThat(saved.getSpotId()).isEqualTo(SPOT_ID);
		assertThat(saved.getSpotName()).isEqualTo("정문 앞");
		assertThat(saved.getStopPoint()).isEqualTo(STOP_POINT);
		assertThat(saved.getMeetingPoint()).isEqualTo(MEETING_POINT);
		assertThat(saved.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getScheduleId, SessionChild::getChildPhase)
				.containsExactlyInAnyOrder(
						tuple(MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING),
						tuple(SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING));
	}

	@Test
	@DisplayName("픽업 시작은 저장한 새 세션의 stateVersion으로 SESSION_STARTED를 저장하고 append가 준 seq 1 이벤트를 발행한다")
	void startAppendsSessionStartedAfterSaveAndPublishesItWithSeqOne() {
		givenNewSessionSaved();
		givenEventAppended();

		startPickupService.start(command(TODAY_KST));

		InOrder inOrder = inOrder(saveSessionPort, sessionEventPort, publishSessionEventPort);
		ArgumentCaptor<PickupSession> savedCaptor = ArgumentCaptor.forClass(PickupSession.class);
		inOrder.verify(saveSessionPort).save(savedCaptor.capture());
		int stateVersion = savedCaptor.getValue().getStateVersion();
		inOrder.verify(sessionEventPort)
				.append(eq(NEW_SESSION_ID), eq("SESSION_STARTED"), eq(stateVersion), eq(NOW), any());
		ArgumentCaptor<SessionEventMessage> publishedCaptor = ArgumentCaptor.forClass(SessionEventMessage.class);
		inOrder.verify(publishSessionEventPort).publish(publishedCaptor.capture());
		verify(sessionEventPort).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort).publish(any());

		SessionEventMessage message = publishedCaptor.getValue();
		assertThat(message.sessionId()).isEqualTo(NEW_SESSION_ID);
		assertThat(message.eventId()).isEqualTo(NEW_EVENT_ID);
		assertThat(message.seq()).isEqualTo(1L);
		assertThat(message.type()).isEqualTo("SESSION_STARTED");
		assertThat(message.stateVersion()).isEqualTo(stateVersion);
		assertThat(message.occurredAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("픽업 시작은 새 세션의 SESSION_STARTED 알림 기록을 가족 보호자와 자녀에게 요청한다")
	void startRequestsSessionStartedNotificationForGuardianAndChildren() {
		givenNewSessionSaved();
		givenEventAppended();

		startPickupService.start(command(TODAY_KST));

		ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(requestCaptor.capture());
		NotificationRequest request = requestCaptor.getValue();
		assertThat(request.sessionId()).isEqualTo(NEW_SESSION_ID);
		assertThat(request.callId()).isNull();
		assertThat(request.type()).isEqualTo("SESSION_STARTED");
		assertThat(request.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("픽업 시작은 새 세션을 만들면 created true와 새 세션의 세션 뷰를 돌려준다")
	void startReturnsNewSessionViewWithCreatedTrue() {
		givenNewSessionSaved();
		givenEventAppended();

		StartPickupResult result = startPickupService.start(command(TODAY_KST));

		PickupSession saved = savedSession();
		assertThat(result.created()).isTrue();
		assertThat(result.session()).isNotNull();
		assertThat(result.session().sessionId()).isEqualTo(NEW_SESSION_ID);
		assertThat(result.session().stateVersion()).isEqualTo(saved.getStateVersion());
		assertThat(result.session().children())
				.extracting(SessionChildView::childUserId, SessionChildView::phase)
				.containsExactlyInAnyOrder(
						tuple(MINJUN_ID, ChildPhase.WAITING),
						tuple(SEOYUN_ID, ChildPhase.WAITING));
	}

	private void givenNewSessionSaved() {
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> withSessionId(invocation.getArgument(0), NEW_SESSION_ID));
	}

	private void givenEventAppended() {
		lenient().when(sessionEventPort.append(anyLong(), anyString(), anyInt(), any(Instant.class), any()))
				.thenAnswer(invocation -> SessionEvent.restore(NEW_EVENT_ID, invocation.<Long>getArgument(0), 1L,
						SessionEventType.valueOf(invocation.<String>getArgument(1)),
						invocation.<Integer>getArgument(2), invocation.<Instant>getArgument(3), Map.of()));
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort).save(captor.capture());
		return captor.getValue();
	}

	private static PickupSession withSessionId(PickupSession session, long sessionId) {
		return PickupSession.restore(sessionId, session.getVehicleId(), session.getFamilyId(), session.getSpotId(),
				session.getSpotName(), session.getStopPoint(), session.getMeetingPoint(), session.getDate(),
				session.getSessionStatus(), session.getVehiclePhase(), session.getSpotStatus(),
				session.getStateVersion(), session.getSpotVersion(), session.getRouteVersion(),
				session.getRouteSpotVersion(), session.isCallHeld(), session.getEtaAt(), session.getRemainingMeters(),
				session.getEtaUpdatedAt(), session.getBoardingDetectedAt(), session.getBoardingCancelableUntil(),
				session.getBoardingDoor(), session.getIncidentType(), session.getIncidentCause(),
				session.getIncidentOpenedAt(), session.getIncidentResolvedAt(), session.getChildren());
	}

	private StartPickupService newService(Clock clock) {
		return new StartPickupService(loadSchedulePort, loadSpotPort, loadChildReadinessPort, loadFamilyPort,
				loadSessionPort, saveSessionPort, loadCallPort, sessionEventPort, publishSessionEventPort,
				recordNotificationPort, clock);
	}

	private void givenSchedules(ScheduleInfo... schedules) {
		lenient().when(loadSchedulePort.loadSchedules(anyList())).thenReturn(List.of(schedules));
	}

	private void givenDefaultSpot(long academyId, Optional<SpotInfo> spot) {
		lenient().when(loadSpotPort.loadDefaultSpot(FAMILY_ID, academyId)).thenReturn(spot);
	}

	private void givenDeviceStatuses(Map<Long, MemberDeviceStatus> statuses) {
		lenient().when(loadChildReadinessPort.loadDeviceStatuses(anyList())).thenReturn(statuses);
	}

	private void givenActiveSessionOfVehicle(PickupSession session) {
		lenient().when(loadSessionPort.loadActiveByVehicle(VEHICLE_ID)).thenReturn(Optional.of(session));
	}

	private static PickupSession activeSession(long sessionId, int stateVersion, LocalDate date,
			SessionChild... children) {
		return PickupSession.restore(sessionId, VEHICLE_ID, FAMILY_ID, SPOT_ID, "정문 앞",
				new LatLng(37.501400, 127.039950), new LatLng(37.501301, 127.039901), date, SessionStatus.ACTIVE,
				VehiclePhase.ROUTING, SpotStatus.SELECTED, stateVersion, 0, 0, null, false, null, null, null, null,
				null, null, null, null, null, null, List.of(children));
	}

	private static SessionChild waitingChild(long sessionChildId, long childUserId, long scheduleId) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.WAITING, 0, null, null, null,
				null, null);
	}

	private static ScheduleInfo schedule(long scheduleId, long childUserId, AcademyInfo academy,
			LocalTime classEndTime, int targetPickup) {
		return schedule(scheduleId, childUserId, academy, classEndTime, targetPickup, ScheduleStatus.READY);
	}

	private static ScheduleInfo schedule(long scheduleId, long childUserId, AcademyInfo academy,
			LocalTime classEndTime, int targetPickup, ScheduleStatus status) {
		return new ScheduleInfo(scheduleId, childUserId, academy, WEEKDAYS, classEndTime, targetPickup, status);
	}

	private static SpotInfo spot(long spotId, long academyId, String name) {
		return new SpotInfo(spotId, FAMILY_ID, academyId, name, STOP_POINT, MEETING_POINT);
	}

	private static StartPickupCommand command(LocalDate date) {
		return command(date, List.of(MINJUN_SCHEDULE_ID, SEOYUN_SCHEDULE_ID));
	}

	private static StartPickupCommand command(LocalDate date, List<Long> scheduleIds) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new StartPickupCommand(vehicle, scheduleIds, date);
	}

	private static void assertRejected(BusinessException exception, String code) {
		assertRejected(exception, HttpStatus.UNPROCESSABLE_CONTENT, code);
	}

	private static void assertRejected(BusinessException exception, HttpStatus status, String code) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(status);
		assertThat(exception.code()).isEqualTo(code);
	}

	private void assertNothingStarted() {
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

}

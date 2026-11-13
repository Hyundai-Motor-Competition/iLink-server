package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotCommand;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.in.SpotCandidate;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveTemporarySpotPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ChangeSpotServiceTest {

	// 한국 시간 2026-11-09(월) 17:40
	private static final Instant NOW = Instant.parse("2026-11-09T08:40:00Z");
	private static final SessionViewAssembler ASSEMBLER = new SessionViewAssembler(PickupProperties.defaults());
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long ACADEMY_ID = 55L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long BACK_GATE_SPOT_ID = 23L;
	private static final long LIVE_CALL_ID = 3001L;

	// 정문으로 한 번 확정(SES-04)한 뒤: CONFIRMED, spotVersion 0, routeVersion 1
	private static final int STATE_VERSION = 5;
	private static final int SPOT_VERSION = 0;
	private static final long LAST_SEQ = 4L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng BACK_STOP_POINT = new LatLng(37.500800, 127.040300);
	private static final LatLng BACK_MEETING_POINT = new LatLng(37.500750, 127.040250);

	// 추천 장소 candidate(#270): 장소 변경 때 임시 장소로 만든다
	private static final long TEMPORARY_SPOT_ID = 30L;
	private static final String CANDIDATE_NAME = "학원 옆 편의점 앞";
	private static final LatLng CANDIDATE_LOCATION = new LatLng(37.500900, 127.040500);

	// 여기로 부르기(#271 · #272): 마지막으로 받은 차량 위치로 임시 장소 「차량 정차 위치」를 만들고 바로 호출한다
	private static final String HERE_SPOT_NAME = "차량 정차 위치";
	private static final LatLng VEHICLE_LOCATION = new LatLng(37.501050, 127.040120);
	private static final Instant VEHICLE_LOCATED_AT = NOW.minusSeconds(10);
	private static final int ROUTE_TOTAL_METERS = 1850;
	private static final long NEW_CALL_ID = 3101L;
	private static final List<Long> NOTIFICATION_IDS = List.of(6001L, 6002L);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private SaveTemporarySpotPort saveTemporarySpotPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	@Mock
	private SaveCallPort saveCallPort;

	@Mock
	private RecordNotificationPort recordNotificationPort;

	private ChangeSpotService changeSpotService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, ASSEMBLER, clock);
		CallEngine callEngine = new CallEngine(loadCallPort, saveCallPort, loadFamilyPort, sessionEventPort,
				publishSessionEventPort, recordNotificationPort, PickupProperties.defaults(), clock);
		changeSpotService = new ChangeSpotService(loadSessionPort, loadSpotPort, saveTemporarySpotPort,
				saveSessionPort, loadFamilyPort, loadCallPort, sessionEventPort, publishSessionEventPort, sessionAccess,
				callEngine, clock);
		lenient().when(saveCallPort.saveCall(any(Call.class))).thenAnswer(invocation -> {
			Call call = invocation.getArgument(0);
			return Call.restore(NEW_CALL_ID, call.getSessionId(), call.getPrevCallId(), call.getTriggerType(),
					call.getStatus(), call.getSpotVersion(), call.getAcceptedAt(), call.getSentAt(),
					call.getDeliveredAt(), call.getAcknowledgedAt(), call.getRevokedAt(), call.getRevokeReason());
		});
		lenient().when(recordNotificationPort.recordNotifications(any(NotificationRequest.class)))
				.thenReturn(NOTIFICATION_IDS);
		lenient().when(loadSpotPort.loadSpot(FRONT_GATE_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(FRONT_GATE_SPOT_ID, FAMILY_ID, ACADEMY_ID, "정문 앞", FRONT_STOP_POINT,
						FRONT_MEETING_POINT)));
		lenient().when(loadSpotPort.loadSpot(BACK_GATE_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(BACK_GATE_SPOT_ID, FAMILY_ID, ACADEMY_ID, "후문 앞", BACK_STOP_POINT,
						BACK_MEETING_POINT)));
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(sessionEventPort.append(anyLong(), anyString(), anyInt(), any(Instant.class), any()))
				.thenAnswer(invocation -> {
					long seq = nextSeq.incrementAndGet();
					SessionEvent event = SessionEvent.restore(EVENT_ID_BASE + seq, invocation.<Long>getArgument(0),
							seq, SessionEventType.valueOf(invocation.<String>getArgument(1)),
							invocation.<Integer>getArgument(2), invocation.<Instant>getArgument(3),
							invocation.getArgument(4));
					appendedEvents.add(event);
					return event;
				});
	}

	@ParameterizedTest(name = "호출 status {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED"})
	@DisplayName("장소 변경은 철회되지 않은 호출(ACCEPTED 포함)이 있으면 CALL_ALREADY_SENT로 거절하고 아무것도 저장 · 발행하지 않는다")
	void changeSpotRejectsLiveCallWithCallAlreadySent(CallStatus liveStatus) {
		givenLockedSession(confirmedFrontGate(calledChildren()));
		givenCalls(liveCall(liveStatus));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@ParameterizedTest(name = "요청 spotVersion {0} · 서버 0")
	@ValueSource(ints = {1, 2})
	@DisplayName("장소 변경은 stateVersion이 같아도 요청 spotVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절하고 아무것도 저장 · 발행하지 않는다")
	void changeSpotRejectsDifferentSpotVersionWithConflictAndLatestView(int requestedSpotVersion) {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID, STATE_VERSION, requestedSpotVersion)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("STATE_VERSION_CONFLICT");
		assertThat(exception.session()).isInstanceOfSatisfying(SessionView.class, latest -> {
			assertThat(latest.sessionId()).isEqualTo(SESSION_ID);
			assertThat(latest.stateVersion()).isEqualTo(STATE_VERSION);
			assertThat(latest.spot().spotId()).isEqualTo(FRONT_GATE_SPOT_ID);
			assertThat(latest.spot().spotVersion()).isEqualTo(SPOT_VERSION);
		});
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("장소 변경은 마지막 호출이 철회(REVOKED)됐으면 거절하지 않고 세션을 새 장소 · SELECTED · spotVersion 1로 저장한다")
	void changeSpotAcceptsWhenLatestCallIsRevoked() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(revokedCall()));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());

		changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotId()).isEqualTo(BACK_GATE_SPOT_ID);
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.SELECTED);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION + 1);
		verify(sessionEventPort).append(eq(SESSION_ID), eq("SPOT_CHANGED"), anyInt(), any(), any());
	}

	@Test
	@DisplayName("장소 변경이 성공하면 세션을 저장한 뒤 SPOT_CHANGED를 저장한 세션의 stateVersion · 지금 시각으로 한 번 저장한다")
	void changeSpotAppendsSpotChangedWithSavedStateVersion() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		PickupSession saved = savedSession();
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(saved.getSpotName()).isEqualTo("후문 앞");
		assertThat(saved.getStopPoint()).isEqualTo(BACK_STOP_POINT);
		assertThat(saved.getMeetingPoint()).isEqualTo(BACK_MEETING_POINT);
		InOrder inOrder = inOrder(saveSessionPort, sessionEventPort);
		inOrder.verify(saveSessionPort, atLeastOnce()).save(any(PickupSession.class));
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SPOT_CHANGED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("SPOT_CHANGED");
	}

	@Test
	@DisplayName("장소 변경은 저장한 SPOT_CHANGED를 append가 준 seq로 보호자와 세션 자녀에게 발행한다")
	void changeSpotPublishesSpotChangedToGuardianAndChildren() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		SessionEvent appended = appendedEventOf("SPOT_CHANGED");
		SessionEventMessage message = publishedMessageOf("SPOT_CHANGED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("장소 변경이 성공하면 바뀐 stateVersion · 새 장소 · SELECTED의 세션 뷰를 돌려준다")
	void changeSpotReturnsViewOfSavedSession() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		SessionView view = changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		assertThat(view.sessionId()).isEqualTo(SESSION_ID);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.status().spot()).isEqualTo(SpotStatus.SELECTED);
		assertThat(view.spot().spotId()).isEqualTo(BACK_GATE_SPOT_ID);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("장소 변경은 끝난(COMPLETED · CANCELLED) 세션이면 SESSION_ALREADY_FINISHED로 거절하고 아무것도 저장 · 발행하지 않는다")
	void changeSpotRejectsFinishedSessionWithSessionAlreadyFinished(SessionStatus finishedStatus) {
		givenLockedSession(finishedFrontGate(finishedStatus));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("candidate 장소 변경은 세션 가족 · 세션 지금 장소의 academyId · candidate 이름 · 정차 지점=만남 지점=candidate 좌표로 임시 장소를 한 번 만든다")
	void changeSpotWithCandidateSavesTemporarySpotOnceWithSessionFamilyAndCurrentSpotAcademy() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));
		givenTemporarySpotSaved();

		changeSpotService.changeSpot(candidateCommand(STATE_VERSION, SPOT_VERSION));

		verify(saveTemporarySpotPort).saveTemporarySpot(FAMILY_ID, ACADEMY_ID, CANDIDATE_NAME, CANDIDATE_LOCATION,
				CANDIDATE_LOCATION);
		verifyNoMoreInteractions(saveTemporarySpotPort);
	}

	@Test
	@DisplayName("candidate 장소 변경은 세션 장소를 받은 임시 spotId로 바꿔(SELECTED, spotVersion +1, 스냅샷 이름 · 좌표가 candidate 값, stateVersion +1) 저장하고 그 세션 뷰를 돌려준다")
	void changeSpotWithCandidateChangesSessionSpotToTemporarySpot() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));
		givenTemporarySpotSaved();

		SessionView view = changeSpotService.changeSpot(candidateCommand(STATE_VERSION, SPOT_VERSION));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotId()).isEqualTo(TEMPORARY_SPOT_ID);
		assertThat(saved.getSpotName()).isEqualTo(CANDIDATE_NAME);
		assertThat(saved.getStopPoint()).isEqualTo(CANDIDATE_LOCATION);
		assertThat(saved.getMeetingPoint()).isEqualTo(CANDIDATE_LOCATION);
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.SELECTED);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION + 1);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.status().spot()).isEqualTo(SpotStatus.SELECTED);
		assertThat(view.spot().spotId()).isEqualTo(TEMPORARY_SPOT_ID);
		assertThat(view.spot().name()).isEqualTo(CANDIDATE_NAME);
		assertThat(view.spot().spotVersion()).isEqualTo(SPOT_VERSION + 1);
	}

	@Test
	@DisplayName("candidate 장소 변경은 SPOT_CHANGED만 저장한 세션의 stateVersion · 지금 시각으로 한 번 저장하고 보호자와 세션 자녀에게 발행한다")
	void changeSpotWithCandidateAppendsAndPublishesSpotChanged() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));
		givenTemporarySpotSaved();

		changeSpotService.changeSpot(candidateCommand(STATE_VERSION, SPOT_VERSION));

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SPOT_CHANGED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("SPOT_CHANGED");
		SessionEventMessage message = publishedMessageOf("SPOT_CHANGED");
		assertThat(message.eventId()).isEqualTo(appendedEventOf("SPOT_CHANGED").getEventId());
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("candidate 장소 변경은 만든 임시 spotId를 place에서 다시 읽어 SPOT_NOT_SAVED 검사를 하지 않는다")
	void changeSpotWithCandidateSkipsSpotNotSavedCheckForTemporarySpot() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));
		lenient().when(saveTemporarySpotPort.saveTemporarySpot(anyLong(), anyLong(), anyString(), any(), any()))
				.thenReturn(TEMPORARY_SPOT_ID);
		lenient().when(loadSpotPort.loadSpot(TEMPORARY_SPOT_ID)).thenReturn(Optional.empty());

		SessionView view = changeSpotService.changeSpot(candidateCommand(STATE_VERSION, SPOT_VERSION));

		assertThat(view.spot().spotId()).isEqualTo(TEMPORARY_SPOT_ID);
		verify(loadSpotPort, never()).loadSpot(TEMPORARY_SPOT_ID);
	}

	@ParameterizedTest(name = "요청 spotVersion {0} · 서버 0")
	@ValueSource(ints = {1, 2})
	@DisplayName("candidate 장소 변경은 요청 spotVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절하고 임시 장소를 만들지 않으며 아무것도 저장 · 발행하지 않는다")
	void changeSpotWithCandidateRejectsDifferentSpotVersionWithoutSavingTemporarySpot(int requestedSpotVersion) {
		givenLockedSession(confirmedFrontGate(waitingChildren()));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(candidateCommand(STATE_VERSION, requestedSpotVersion)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("STATE_VERSION_CONFLICT");
		assertThat(exception.session()).isInstanceOfSatisfying(SessionView.class, latest -> {
			assertThat(latest.spot().spotId()).isEqualTo(FRONT_GATE_SPOT_ID);
			assertThat(latest.spot().spotVersion()).isEqualTo(SPOT_VERSION);
		});
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("candidate 장소 변경은 끝난(COMPLETED · CANCELLED) 세션이면 SESSION_ALREADY_FINISHED로 거절하고 임시 장소를 만들지 않으며 아무것도 저장 · 발행하지 않는다")
	void changeSpotWithCandidateRejectsFinishedSessionWithoutSavingTemporarySpot(SessionStatus finishedStatus) {
		givenLockedSession(finishedFrontGate(finishedStatus));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(candidateCommand(STATE_VERSION, SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("candidate 장소 변경은 이동 중(MEETING) 자녀가 있으면 CHILD_ALREADY_MOVING으로 거절하고 임시 장소를 만들지 않으며 아무것도 저장 · 발행하지 않는다")
	void changeSpotWithCandidateRejectsMovingChildWithoutSavingTemporarySpot() {
		givenLockedSession(confirmedFrontGate(meetingChildren()));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(candidateCommand(STATE_VERSION, SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CHILD_ALREADY_MOVING");
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@ParameterizedTest(name = "호출 status {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED"})
	@DisplayName("candidate 장소 변경은 철회되지 않은 호출이 있으면 CALL_ALREADY_SENT로 거절하고 임시 장소를 만들지 않으며 아무것도 저장 · 발행하지 않는다")
	void changeSpotWithCandidateRejectsLiveCallWithoutSavingTemporarySpot(CallStatus liveStatus) {
		givenLockedSession(confirmedFrontGate(calledChildren()));
		givenCalls(liveCall(liveStatus));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(candidateCommand(STATE_VERSION, SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("여기로 부르기(here=true)는 세션 가족 · 지금 장소의 academyId · 이름 「차량 정차 위치」 · 정차 지점=만남 지점=마지막 차량 위치로 임시 장소를 한 번 만든다")
	void changeSpotWithHereSavesTemporarySpotOnceAtLastVehicleLocation() {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), readyChildren()));
		givenTemporarySpotSaved();

		changeSpotService.changeSpot(hereCommand(SPOT_VERSION));

		verify(saveTemporarySpotPort).saveTemporarySpot(FAMILY_ID, ACADEMY_ID, HERE_SPOT_NAME, VEHICLE_LOCATION,
				VEHICLE_LOCATION);
		verifyNoMoreInteractions(saveTemporarySpotPort);
	}

	@Test
	@DisplayName("여기로 부르기는 세션 장소를 임시 spotId · 「차량 정차 위치」 · 차량 위치로 바꾸고 바로 확정해(CONFIRMED, spotVersion +1, routeVersion +1, routeTotalMeters 그대로, stateVersion +2) 저장하고 그 세션 뷰를 돌려준다")
	void changeSpotWithHereChangesSpotToTemporarySpotAndConfirmsRoute() {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), readyChildren()));
		givenTemporarySpotSaved();

		SessionView view = changeSpotService.changeSpot(hereCommand(SPOT_VERSION));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotId()).isEqualTo(TEMPORARY_SPOT_ID);
		assertThat(saved.getSpotName()).isEqualTo(HERE_SPOT_NAME);
		assertThat(saved.getStopPoint()).isEqualTo(VEHICLE_LOCATION);
		assertThat(saved.getMeetingPoint()).isEqualTo(VEHICLE_LOCATION);
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION + 1);
		assertThat(saved.getRouteVersion()).isEqualTo(2);
		assertThat(saved.getRouteSpotVersion()).isEqualTo(SPOT_VERSION + 1);
		assertThat(saved.getRouteTotalMeters()).isEqualTo(ROUTE_TOTAL_METERS);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 2);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 2);
		assertThat(view.status().spot()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(view.spot().spotId()).isEqualTo(TEMPORARY_SPOT_ID);
		assertThat(view.spot().name()).isEqualTo(HERE_SPOT_NAME);
		assertThat(view.spot().spotVersion()).isEqualTo(SPOT_VERSION + 1);
	}

	@ParameterizedTest(name = "합류 안내 방식 {0}")
	@EnumSource(JoinCallMode.class)
	@DisplayName("여기로 부르기는 가족의 합류 안내 방식(AUTO · GUARDIAN_CONFIRM)과 상관없이 바뀐 spotVersion으로 triggerType MANUAL · ACCEPTED 호출을 한 번 저장한다")
	void changeSpotWithHereSavesManualAcceptedCallRegardlessOfJoinCallMode(JoinCallMode joinCallMode) {
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(new FamilyInfo(FAMILY_ID, joinCallMode, 0));
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), readyChildren()));
		givenTemporarySpotSaved();

		changeSpotService.changeSpot(hereCommand(SPOT_VERSION));

		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		Call call = captor.getValue();
		assertThat(call.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(call.getTriggerType()).isEqualTo(CallTriggerType.MANUAL);
		assertThat(call.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(call.getSpotVersion()).isEqualTo(SPOT_VERSION + 1);
		assertThat(call.getPrevCallId()).isNull();
	}

	@Test
	@DisplayName("여기로 부르기는 저장한 호출의 callId로 세션 자녀 둘에게 CALL 알림 기록을 한 번 요청한다")
	void changeSpotWithHereRecordsCallNotificationForSessionChildren() {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), readyChildren()));
		givenTemporarySpotSaved();

		changeSpotService.changeSpot(hereCommand(SPOT_VERSION));

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(captor.capture());
		NotificationRequest request = captor.getValue();
		assertThat(request.sessionId()).isEqualTo(SESSION_ID);
		assertThat(request.callId()).isEqualTo(NEW_CALL_ID);
		assertThat(request.type()).isEqualTo("CALL");
		assertThat(request.receiverUserIds()).containsExactlyInAnyOrder(MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("여기로 부르기는 SPOT_CHANGED(stateVersion +1) · SPOT_CONFIRMED(+2) · CALL_TRIGGERED(+2, callId · trigger MANUAL) 순서로 지금 시각에 저장하고 같은 순서로 발행한다")
	void changeSpotWithHereAppendsAndPublishesSpotChangedSpotConfirmedCallTriggeredInOrder() {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), readyChildren()));
		givenTemporarySpotSaved();

		changeSpotService.changeSpot(hereCommand(SPOT_VERSION));

		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("SPOT_CHANGED", "SPOT_CONFIRMED", "CALL_TRIGGERED");
		assertThat(appendedEvents)
				.extracting(SessionEvent::getStateVersion)
				.containsExactly(STATE_VERSION + 1, STATE_VERSION + 2, STATE_VERSION + 2);
		assertThat(appendedEvents).extracting(SessionEvent::getOccurredAt).containsOnly(NOW);
		assertThat(appendedEventOf("CALL_TRIGGERED").getPayload())
				.containsEntry("callId", NEW_CALL_ID)
				.containsEntry("trigger", "MANUAL");
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		assertThat(captor.getAllValues())
				.extracting(SessionEventMessage::type)
				.containsExactly("SPOT_CHANGED", "SPOT_CONFIRMED", "CALL_TRIGGERED");
		assertThat(publishedMessageOf("SPOT_CONFIRMED").receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
		assertThat(publishedMessageOf("CALL_TRIGGERED").payload()).containsEntry("notificationIds", NOTIFICATION_IDS);
	}

	@Test
	@DisplayName("여기로 부르기는 지금 장소가 확정 전(SELECTED · 경로가 이전 장소 버전)이어도 바로 확정하므로 G2 · G3을 통과로 보고 MANUAL 호출을 만든다")
	void changeSpotWithHereOnSelectedSpotTreatsG2AndG3AsMetAndSavesCall() {
		givenLockedSession(selectedStoppedFrontGate());
		givenTemporarySpotSaved();

		changeSpotService.changeSpot(hereCommand(SPOT_VERSION + 1));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION + 2);
		assertThat(saved.getRouteSpotVersion()).isEqualTo(SPOT_VERSION + 2);
		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		assertThat(captor.getValue().getTriggerType()).isEqualTo(CallTriggerType.MANUAL);
		assertThat(captor.getValue().getSpotVersion()).isEqualTo(SPOT_VERSION + 2);
	}

	@Test
	@DisplayName("여기로 부르기는 받은 차량 위치가 없으면 422 VALIDATION_FAILED(fields here)로 거절하고 임시 장소 · 세션 · 호출 · 알림 · 이벤트를 만들지 않는다")
	void changeSpotWithHereWithoutVehicleLocationRejectsWithValidationFailed() {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, null, NOW.minusSeconds(10),
				readyChildren()));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(hereCommand(SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
		assertThat(exception.fields()).extracting(field -> field.field()).containsExactly("here");
		verifyNothingSaved();
	}

	@Test
	@DisplayName("여기로 부르기는 차량 위치가 없고 정차 중도 아니면 가드보다 차량 위치를 먼저 보아 422 VALIDATION_FAILED로 거절한다")
	void changeSpotWithHereChecksVehicleLocationBeforeCallGuard() {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, null,
				NOW.minusSeconds(10), readyChildren()));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(hereCommand(SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
		verifyNothingSaved();
	}

	static Stream<Arguments> hereGuardUnmetSessions() {
		return Stream.of(
				Arguments.of("차량 ROUTING", stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.ROUTING,
						VEHICLE_LOCATION, NOW.minusSeconds(10), readyChildren()), List.of("G6")),
				Arguments.of("차량 APPROACHING", stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
						VEHICLE_LOCATION, NOW.minusSeconds(10), readyChildren()), List.of("G6")),
				Arguments.of("차량 DEPARTED", stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.DEPARTED,
						VEHICLE_LOCATION, NOW.minusSeconds(10), readyChildren()), List.of("G6")),
				Arguments.of("세션 ON_HOLD", stoppedFrontGate(SessionStatus.ON_HOLD, VehiclePhase.STOPPED,
						VEHICLE_LOCATION, NOW.minusSeconds(10), readyChildren()), List.of("G1")),
				Arguments.of("ETA 61초 전", stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED,
						VEHICLE_LOCATION, NOW.minusSeconds(61), readyChildren()), List.of("G4")),
				Arguments.of("준비 시간 안 지남", stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED,
						VEHICLE_LOCATION, NOW.minusSeconds(10), preparingChildren()), List.of("G5")));
	}

	@ParameterizedTest(name = "{0} → unmet {2}")
	@MethodSource("hereGuardUnmetSessions")
	@DisplayName("여기로 부르기는 장소를 바꾼 뒤 상태로 본 가드(G1 · G4 · G5 · G6)를 못 넘으면 409 CALL_GUARD_UNMET(error.unmet)로 거절하고 세션 장소를 그대로 두며 임시 장소 · 세션 · 호출 · 알림 · 이벤트를 만들지 않는다")
	void changeSpotWithHereRejectsUnmetCallGuardWithConflict(String caseName, PickupSession session,
			List<String> expectedUnmet) {
		givenLockedSession(session);
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(hereCommand(SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_GUARD_UNMET");
		assertThat(exception.details()).containsOnly(entry("unmet", expectedUnmet));
		assertThat(session.getSpotId()).isEqualTo(FRONT_GATE_SPOT_ID);
		assertThat(session.getSpotName()).isEqualTo("정문 앞");
		assertThat(session.getSpotVersion()).isEqualTo(SPOT_VERSION);
		assertThat(session.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(session.getStateVersion()).isEqualTo(STATE_VERSION);
		verifyNothingSaved();
	}

	@ParameterizedTest(name = "호출 status {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED"})
	@DisplayName("여기로 부르기는 철회되지 않은 호출이 있으면 409 CALL_ALREADY_SENT로 거절하고 임시 장소 · 세션 · 호출 · 알림 · 이벤트를 만들지 않는다")
	void changeSpotWithHereRejectsLiveCallWithCallAlreadySent(CallStatus liveStatus) {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), calledChildren()));
		givenCalls(liveCall(liveStatus));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(hereCommand(SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNothingSaved();
	}

	@Test
	@DisplayName("여기로 부르기는 이동 중(MEETING) 자녀가 있으면 409 CHILD_ALREADY_MOVING으로 거절하고 임시 장소 · 세션 · 호출 · 알림 · 이벤트를 만들지 않는다")
	void changeSpotWithHereRejectsMovingChildWithChildAlreadyMoving() {
		givenLockedSession(stoppedFrontGate(SessionStatus.ACTIVE, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), meetingChildren()));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(hereCommand(SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CHILD_ALREADY_MOVING");
		verifyNothingSaved();
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("여기로 부르기는 끝난(COMPLETED · CANCELLED) 세션이면 409 SESSION_ALREADY_FINISHED로 거절하고 아무것도 만들지 않는다")
	void changeSpotWithHereRejectsFinishedSession(SessionStatus finishedStatus) {
		givenLockedSession(stoppedFrontGate(finishedStatus, VehiclePhase.STOPPED, VEHICLE_LOCATION,
				NOW.minusSeconds(10), readyChildren()));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(hereCommand(SPOT_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNothingSaved();
	}

	private void verifyNothingSaved() {
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, saveCallPort, recordNotificationPort,
				sessionEventPort, publishSessionEventPort);
	}

	private static ChangeSpotCommand hereCommand(int spotVersion) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return ChangeSpotCommand.ofHere(vehicle, SESSION_ID, STATE_VERSION, spotVersion);
	}

	// 정문으로 확정(CONFIRMED, spotVersion 0, routeVersion 1, 경로 총 1850m)한 세션. 차량 위치 · 정차 · ETA를 고른다
	private static PickupSession stoppedFrontGate(SessionStatus sessionStatus, VehiclePhase vehiclePhase,
			LatLng vehicleLocation, Instant etaUpdatedAt, List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, sessionStatus, vehiclePhase, SpotStatus.CONFIRMED,
				STATE_VERSION, SPOT_VERSION, 1, SPOT_VERSION, false, etaUpdatedAt, 0, etaUpdatedAt, null, null, null,
				null, null, null, null, null, vehicleLocation, vehicleLocation == null ? null : VEHICLE_LOCATED_AT,
				ROUTE_TOTAL_METERS, children);
	}

	// 정문으로 확정한 뒤 장소를 바꿔 아직 확정 전: SELECTED, spotVersion 1, 경로는 spotVersion 0의 것. 정차 · ETA 신선
	private static PickupSession selectedStoppedFrontGate() {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.STOPPED,
				SpotStatus.SELECTED, STATE_VERSION, SPOT_VERSION + 1, 1, SPOT_VERSION, false, NOW.minusSeconds(10), 0,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, VEHICLE_LOCATION,
				VEHICLE_LOCATED_AT, ROUTE_TOTAL_METERS, readyChildren());
	}

	// 준비 시간이 지났거나(민준 1분 전) 없는(서윤) 대기 자녀
	private static List<SessionChild> readyChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0,
						NOW.minus(Duration.ofMinutes(1)), null, null, null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	// 서윤의 준비 시간(callNotBefore)이 2분 뒤라 아직 안 지남
	private static List<SessionChild> preparingChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0,
						NOW.minus(Duration.ofMinutes(1)), null, null, null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0,
						NOW.plus(Duration.ofMinutes(2)), null, null, null, null));
	}

	// 임시 장소 포트가 TEMPORARY_SPOT_ID를 돌려주고, 그 id를 place에서 다시 읽으면 candidate 값의 임시 장소가 나온다
	private void givenTemporarySpotSaved() {
		lenient().when(saveTemporarySpotPort.saveTemporarySpot(anyLong(), anyLong(), anyString(), any(), any()))
				.thenReturn(TEMPORARY_SPOT_ID);
		lenient().when(loadSpotPort.loadSpot(TEMPORARY_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(TEMPORARY_SPOT_ID, FAMILY_ID, ACADEMY_ID, CANDIDATE_NAME, CANDIDATE_LOCATION,
						CANDIDATE_LOCATION)));
	}

	private static ChangeSpotCommand candidateCommand(int stateVersion, int spotVersion) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return ChangeSpotCommand.ofCandidate(vehicle, SESSION_ID, new SpotCandidate(CANDIDATE_NAME, CANDIDATE_LOCATION),
				stateVersion, spotVersion);
	}

	private static List<SessionChild> meetingChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void givenCalls(Call call) {
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(call));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call));
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort, atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}

	private SessionEvent appendedEventOf(String type) {
		List<SessionEvent> matched = appendedEvents.stream()
				.filter(event -> event.getType().name().equals(type))
				.toList();
		assertThat(matched).as("저장한 %s 이벤트", type).hasSize(1);
		return matched.get(0);
	}

	private SessionEventMessage publishedMessageOf(String type) {
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		List<SessionEventMessage> matched = captor.getAllValues().stream()
				.filter(message -> type.equals(message.type()))
				.toList();
		assertThat(matched).as("발행한 %s 이벤트", type).hasSize(1);
		return matched.get(0);
	}

	private static PickupSession confirmedFrontGate(List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, 1, SPOT_VERSION, false,
				NOW.plus(Duration.ofMinutes(4)), 1200, NOW.minusSeconds(10), null, null, null, null, null, null, null, null,
				children);
	}

	// 정문으로 확정한 뒤 끝난(완료 · 취소) 세션. stateVersion · spotVersion은 요청과 같다
	private static PickupSession finishedFrontGate(SessionStatus finishedStatus) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, finishedStatus, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, 1, SPOT_VERSION, false,
				NOW.plus(Duration.ofMinutes(4)), 1200, NOW.minusSeconds(10), null, null, null, null, null, null, null, null,
				waitingChildren());
	}

	private static List<SessionChild> waitingChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	private static List<SessionChild> calledChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null));
	}

	private static Call liveCall(CallStatus status) {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(2));
		Instant sentAt = status == CallStatus.ACCEPTED ? null : acceptedAt.plusSeconds(1);
		Instant deliveredAt = status == CallStatus.DELIVERED ? acceptedAt.plusSeconds(3) : null;
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, SPOT_VERSION,
				acceptedAt, sentAt, deliveredAt, null, null, null);
	}

	private static Call revokedCall() {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(5));
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.REVOKED,
				SPOT_VERSION, acceptedAt, acceptedAt.plusSeconds(1), null, null, acceptedAt.plusSeconds(60),
				"장소를 바꾸려고 철회");
	}

	private static ChangeSpotCommand command(long spotId) {
		return command(spotId, STATE_VERSION, SPOT_VERSION);
	}

	private static ChangeSpotCommand command(long spotId, int stateVersion, int spotVersion) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new ChangeSpotCommand(vehicle, SESSION_ID, spotId, stateVersion, spotVersion);
	}

}

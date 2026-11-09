package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CallHoldCommand;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
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
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HoldCallServiceTest {

	// 한국 시간 2026-11-09(월) 18:09
	private static final Instant NOW = Instant.parse("2026-11-09T09:09:00Z");
	private static final SessionViewAssembler ASSEMBLER = new SessionViewAssembler(PickupProperties.defaults());
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	// 마지막 SIG-01은 10초 전에 왔다(G4 ETA 유효 1분 안)
	private static final Instant LAST_OBSERVED_AT = NOW.minusSeconds(10);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long PREV_CALL_ID = 3300L;
	private static final long NEW_CALL_ID = 3301L;

	// 장소를 한 번 바꾼 뒤 다시 확정한 세션: spotVersion 1, 확정 경로도 spotVersion 1(G3 통과)
	private static final int STATE_VERSION = 9;
	private static final int SPOT_VERSION = 1;
	private static final int ROUTE_VERSION = 2;
	private static final long LAST_SEQ = 7L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);

	private static final PickupProperties PROPERTIES = new PickupProperties(Duration.ofMinutes(1),
			Duration.ofSeconds(180), Duration.ofSeconds(10), 700, Duration.ofSeconds(60), Duration.ofMinutes(15), 90);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private SaveCallPort saveCallPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	@Mock
	private RecordNotificationPort recordNotificationPort;

	private HoldCallService holdCallService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, ASSEMBLER, clock);
		CallEngine callEngine = new CallEngine(loadCallPort, saveCallPort, loadFamilyPort, sessionEventPort,
				publishSessionEventPort, recordNotificationPort, PROPERTIES, clock);
		holdCallService = new HoldCallService(loadSessionPort, saveSessionPort, loadCallPort, loadFamilyPort,
				sessionAccess, callEngine, clock);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(saveCallPort.saveCall(any(Call.class))).thenAnswer(invocation -> {
			Call call = invocation.getArgument(0);
			return Call.restore(NEW_CALL_ID, call.getSessionId(), call.getPrevCallId(), call.getTriggerType(),
					call.getStatus(), call.getSpotVersion(), call.getAcceptedAt(), call.getSentAt(),
					call.getDeliveredAt(), call.getAcknowledgedAt(), call.getRevokedAt(), call.getRevokeReason());
		});
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
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("호출 보류는 철회되지 않은 호출(ACCEPTED 포함)이 있으면 409 CALL_ALREADY_SENT로 거절하고 세션 · 호출 · 이벤트를 저장 · 발행하지 않는다")
	void holdCallRejectsLiveCallWithCallAlreadySent(CallStatus liveStatus) {
		givenLockedSession(session(false, NOW.plusSeconds(590), 4200, LAST_OBSERVED_AT));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(previousCall(liveStatus)));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(previousCall(liveStatus)));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> holdCallService.holdCall(command()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNoInteractions(saveSessionPort, saveCallPort, sessionEventPort, publishSessionEventPort,
				recordNotificationPort);
	}

	@Test
	@DisplayName("호출 보류는 마지막 호출이 철회(REVOKED)됐으면 거절하지 않고 callHeld true · stateVersion 1 올려 저장하고 응답 세션 뷰 call.held도 true다")
	void holdCallSavesCallHeldWhenLatestCallIsRevoked() {
		givenLockedSession(session(false, NOW.plusSeconds(590), 4200, LAST_OBSERVED_AT));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID))
				.thenReturn(Optional.of(previousCall(CallStatus.REVOKED)));

		SessionView view = holdCallService.holdCall(command());

		PickupSession saved = savedSession();
		assertThat(saved.isCallHeld()).isTrue();
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.call().held()).isTrue();
	}

	@Test
	@DisplayName("호출 보류 해제는 callHeld false · stateVersion 1 올려 저장하고 응답 세션 뷰 call.held도 false다")
	void releaseCallHoldSavesCallHeldFalseAndRaisesStateVersion() {
		givenLockedSession(session(true, LAST_OBSERVED_AT.plusSeconds(600), 4200, LAST_OBSERVED_AT));

		SessionView view = holdCallService.releaseCallHold(command());

		PickupSession saved = savedSession();
		assertThat(saved.isCallHeld()).isFalse();
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.call().held()).isFalse();
	}

	@Test
	@DisplayName("호출 보류 해제는 저장된 ETA가 180초 안이고 가드 G1~G5를 통과하면 Call을 ACCEPTED · ETA_3MIN · 지금 spotVersion · prevCallId 없음으로 한 번 저장한다")
	void releaseCallHoldCreatesEtaTriggeredCallFromStoredEta() {
		givenLockedSession(session(true, LAST_OBSERVED_AT.plusSeconds(150), 1500, LAST_OBSERVED_AT));

		holdCallService.releaseCallHold(command());

		Call saved = savedCall();
		assertThat(saved.getCallId()).isNull();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.ETA_3MIN);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION);
		assertThat(saved.getPrevCallId()).isNull();
	}

	@Test
	@DisplayName("호출 보류 해제는 저장된 남은 거리가 700m 안이면 ETA가 길어도 triggerType DIST_700M인 ACCEPTED Call을 저장한다")
	void releaseCallHoldCreatesDistanceTriggeredCallFromStoredRemainingMeters() {
		givenLockedSession(session(true, LAST_OBSERVED_AT.plusSeconds(600), 650, LAST_OBSERVED_AT));

		holdCallService.releaseCallHold(command());

		Call saved = savedCall();
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.DIST_700M);
	}

	@Test
	@DisplayName("호출 보류 해제로 호출을 만들면 세션 자녀에게 CALL 알림 기록을 요청하고 CALL_TRIGGERED를 올린 stateVersion · 지금 시각으로 저장해 append가 준 eventId · seq로 보호자에게 발행한다")
	void releaseCallHoldRecordsNotificationAndAppendsCallTriggeredWithRaisedStateVersion() {
		givenLockedSession(session(true, LAST_OBSERVED_AT.plusSeconds(150), 1500, LAST_OBSERVED_AT));

		holdCallService.releaseCallHold(command());

		ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(requestCaptor.capture());
		NotificationRequest request = requestCaptor.getValue();
		assertThat(request.sessionId()).isEqualTo(SESSION_ID);
		assertThat(request.callId()).isEqualTo(NEW_CALL_ID);
		assertThat(request.type()).isEqualTo("CALL");
		assertThat(request.receiverUserIds()).containsExactlyInAnyOrder(MINJUN_ID, SEOYUN_ID);

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_TRIGGERED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("CALL_TRIGGERED");
		SessionEvent appended = appendedEvents.get(0);
		assertThat(appended.getPayload())
				.containsEntry("callId", NEW_CALL_ID)
				.containsEntry("trigger", "ETA_3MIN");

		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages).extracting(SessionEventMessage::type).containsExactly("CALL_TRIGGERED");
		SessionEventMessage message = messages.get(0);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("호출 보류 해제는 저장된 ETA(180초 넘음) · 남은 거리(700m 넘음)가 트리거에 못 미치면 callHeld만 풀고 Call · 알림 기록 요청 · CALL_TRIGGERED를 만들지 않는다")
	void releaseCallHoldDoesNotCreateCallWhenStoredEtaMissesTrigger() {
		givenLockedSession(session(true, LAST_OBSERVED_AT.plusSeconds(600), 4200, LAST_OBSERVED_AT));

		holdCallService.releaseCallHold(command());

		assertThat(savedSession().isCallHeld()).isFalse();
		assertNoCallCreated();
	}

	@Test
	@DisplayName("호출 보류 해제는 저장된 ETA가 트리거를 만족해도 1분 넘게 지난 값이면(G4) callHeld만 풀고 호출을 만들지 않는다")
	void releaseCallHoldDoesNotCreateCallWhenStoredEtaIsStale() {
		Instant staleObservedAt = NOW.minus(Duration.ofMinutes(2));
		givenLockedSession(session(true, staleObservedAt.plusSeconds(150), 600, staleObservedAt));

		holdCallService.releaseCallHold(command());

		assertThat(savedSession().isCallHeld()).isFalse();
		assertNoCallCreated();
	}

	@Test
	@DisplayName("호출 보류 해제는 아직 받은 ETA가 없는 세션(etaAt · remainingMeters · etaUpdatedAt 없음)이면 오류 없이 callHeld만 풀고 호출을 만들지 않는다")
	void releaseCallHoldWithoutStoredEtaDoesNotCreateCall() {
		givenLockedSession(session(true, null, null, null));

		holdCallService.releaseCallHold(command());

		assertThat(savedSession().isCallHeld()).isFalse();
		assertNoCallCreated();
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void assertNoCallCreated() {
		verify(saveCallPort, never()).saveCall(any());
		verify(recordNotificationPort, never()).recordNotifications(any());
		verify(sessionEventPort, never()).append(anyLong(), eq("CALL_TRIGGERED"), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort, atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}

	private Call savedCall() {
		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		return captor.getValue();
	}

	private List<SessionEventMessage> publishedMessages() {
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		return captor.getAllValues();
	}

	// ACTIVE · 경로 확정(CONFIRMED) · 두 자녀 실내 대기(준비 시간 추가 없음)인 세션. ETA 값만 바꿔 쓴다
	private static PickupSession session(boolean callHeld, Instant etaAt, Integer remainingMeters,
			Instant etaUpdatedAt) {
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, ROUTE_VERSION, SPOT_VERSION, callHeld, etaAt,
				remainingMeters, etaUpdatedAt, null, null, null, null, null, null, null, null, children);
	}

	private static Call previousCall(CallStatus status) {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(3));
		Instant revokedAt = status == CallStatus.REVOKED ? acceptedAt.plusSeconds(60) : null;
		return Call.restore(PREV_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, SPOT_VERSION,
				acceptedAt, acceptedAt.plusSeconds(1), null, null, revokedAt, null);
	}

	private static CallHoldCommand command() {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
		return new CallHoldCommand(guardian, SESSION_ID, STATE_VERSION);
	}

}

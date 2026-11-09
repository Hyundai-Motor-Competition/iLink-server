package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CallResult;
import com.example.pickupservice.pickup.application.port.in.SendCallCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.GuardId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SendCallServiceTest {

	// 한국 시간 2026-11-09(월) 18:09
	private static final Instant NOW = Instant.parse("2026-11-09T09:09:00Z");
	private static final SessionViewAssembler ASSEMBLER = new SessionViewAssembler(PickupProperties.defaults());
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long LIVE_CALL_ID = 3001L;
	private static final long REVOKED_CALL_ID = 3002L;
	private static final long NEW_CALL_ID = 3003L;

	// 장소를 한 번 바꾼 뒤(spotVersion 1) 정문으로 다시 확정(SES-04)한 상태
	private static final int STATE_VERSION = 8;
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

	private SendCallService sendCallService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, ASSEMBLER, clock);
		CallEngine callEngine = new CallEngine(loadCallPort, saveCallPort, loadFamilyPort, sessionEventPort,
				publishSessionEventPort, recordNotificationPort, PROPERTIES, clock);
		sendCallService = new SendCallService(loadSessionPort, loadCallPort, loadFamilyPort, sessionAccess,
				callEngine, clock);
		givenJoinCallMode(JoinCallMode.GUARDIAN_CONFIRM);
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.empty());
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

	@Test
	@DisplayName("합류 안내 보내기는 가족의 합류 안내 방식이 AUTO면 409 NOT_GUARDIAN_CONFIRM_MODE로 거절하고 Call · 알림 기록 요청 · 이벤트를 만들지 않는다")
	void sendRejectsAutoFamilyWithNotGuardianConfirmMode() {
		givenJoinCallMode(JoinCallMode.AUTO);
		givenLockedSession(readyForManualCall().build());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> sendCallService.send(command()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("NOT_GUARDIAN_CONFIRM_MODE");
		verifyNoInteractions(saveCallPort, recordNotificationPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("합류 안내 보내기는 AUTO 가족이면 철회되지 않은 호출이 있고 가드를 통과하지 못해도 NOT_GUARDIAN_CONFIRM_MODE로 먼저 거절한다")
	void sendChecksJoinCallModeBeforeLiveCallAndGuard() {
		givenJoinCallMode(JoinCallMode.AUTO);
		givenLockedSession(readyForManualCall().etaUpdatedAt(NOW.minusSeconds(61)).build());
		givenLiveCall(liveCall(CallStatus.SENT));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> sendCallService.send(command()));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("NOT_GUARDIAN_CONFIRM_MODE");
		verifyNoInteractions(saveCallPort, recordNotificationPort, sessionEventPort, publishSessionEventPort);
	}

	@ParameterizedTest(name = "가드 {0} 미충족")
	@EnumSource(GuardId.class)
	@DisplayName("합류 안내 보내기는 가드 하나라도 통과하지 못하면 422 CALL_GUARD_UNMET과 error 추가 키 unmet에 그 가드 id를 담아 거절하고 아무것도 저장 · 발행하지 않는다")
	void sendRejectsUnmetGuardWithCallGuardUnmet(GuardId unmetGuard) {
		givenLockedSession(breaking(unmetGuard));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> sendCallService.send(command()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("CALL_GUARD_UNMET");
		assertThat(exception.details()).containsOnlyKeys("unmet");
		assertThat(exception.details().get("unmet")).isEqualTo(List.of(unmetGuard.name()));
		verifyNoInteractions(saveCallPort, recordNotificationPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("합류 안내 보내기는 장소 미확정(G2)과 ETA 오래됨(G4)이 함께면 unmet에 [G2, G4]를 가드 순서대로 담아 CALL_GUARD_UNMET으로 거절한다")
	void sendRejectsWithAllUnmetGuardsInOrder() {
		givenLockedSession(readyForManualCall()
				.spotStatus(SpotStatus.SELECTED)
				.etaUpdatedAt(NOW.minusSeconds(61))
				.build());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> sendCallService.send(command()));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("CALL_GUARD_UNMET");
		assertThat(exception.details().get("unmet")).isEqualTo(List.of("G2", "G4"));
	}

	@ParameterizedTest(name = "살아 있는 호출 status {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("합류 안내 보내기는 철회되지 않은 호출(ACCEPTED 포함)이 있으면 409 CALL_ALREADY_SENT로 거절하고 새 Call · 알림 기록 요청 · 이벤트를 만들지 않는다")
	void sendRejectsLiveCallWithCallAlreadySent(CallStatus liveStatus) {
		givenLockedSession(readyForManualCall().build());
		givenLiveCall(liveCall(liveStatus));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> sendCallService.send(command()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNoInteractions(saveCallPort, recordNotificationPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("합류 안내 보내기는 이미 보낸 호출이 있으면 ETA가 오래돼 가드(G4)를 통과하지 못해도 CALL_GUARD_UNMET이 아니라 CALL_ALREADY_SENT로 거절한다")
	void sendChecksLiveCallBeforeGuard() {
		givenLockedSession(readyForManualCall().etaUpdatedAt(NOW.minusSeconds(61)).build());
		givenLiveCall(liveCall(CallStatus.SENT));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> sendCallService.send(command()));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNoInteractions(saveCallPort, recordNotificationPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("합류 안내 보내기는 GUARDIAN_CONFIRM 가족에서 가드 G1~G5를 통과하면 ETA · 거리 트리거(180초 · 700m) 밖이어도 Call을 ACCEPTED · MANUAL · 지금 spotVersion · prevCallId 없음으로 한 번 저장한다")
	void sendSavesAcceptedManualCallWhenGuardsPass() {
		givenLockedSession(readyForManualCall().build());

		sendCallService.send(command());

		Call saved = savedCall();
		assertThat(saved.getCallId()).isNull();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.MANUAL);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION);
		assertThat(saved.getPrevCallId()).isNull();
	}

	@Test
	@DisplayName("합류 안내 보내기가 성공하면 저장한 Call의 callId와 status ACCEPTED를 돌려준다")
	void sendReturnsSavedCallIdWithAcceptedStatus() {
		givenLockedSession(readyForManualCall().build());

		CallResult result = sendCallService.send(command());

		assertThat(result).isEqualTo(new CallResult(NEW_CALL_ID, CallStatus.ACCEPTED));
	}

	@Test
	@DisplayName("합류 안내 보내기는 마지막 호출이 철회(REVOKED)됐으면 거절하지 않고 그 callId를 prevCallId로 단 MANUAL Call을 저장한다")
	void sendLinksPrevCallIdWhenLatestCallIsRevoked() {
		givenLockedSession(readyForManualCall().build());
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(revokedCall()));

		CallResult result = sendCallService.send(command());

		Call saved = savedCall();
		assertThat(saved.getPrevCallId()).isEqualTo(REVOKED_CALL_ID);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.MANUAL);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(result.callId()).isEqualTo(NEW_CALL_ID);
	}

	@Test
	@DisplayName("합류 안내 보내기가 성공하면 Call을 저장한 뒤 그 callId로 세션 자녀 모두에게 CALL 알림 기록을 한 번 요청한다")
	void sendRequestsCallNotificationForSessionChildrenAfterSavingCall() {
		givenLockedSession(readyForManualCall().build());

		sendCallService.send(command());

		InOrder inOrder = inOrder(saveCallPort, recordNotificationPort);
		inOrder.verify(saveCallPort).saveCall(any(Call.class));
		inOrder.verify(recordNotificationPort).recordNotifications(any(NotificationRequest.class));
		ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(requestCaptor.capture());
		NotificationRequest request = requestCaptor.getValue();
		assertThat(request.sessionId()).isEqualTo(SESSION_ID);
		assertThat(request.callId()).isEqualTo(NEW_CALL_ID);
		assertThat(request.type()).isEqualTo("CALL");
		assertThat(request.receiverUserIds()).containsExactlyInAnyOrder(MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("합류 안내 보내기가 성공하면 Call을 저장한 뒤 CALL_TRIGGERED를 지금 stateVersion · 서버 시각과 payload callId · trigger MANUAL로 한 번 저장한다")
	void sendAppendsCallTriggeredWithManualTriggerAfterSavingCall() {
		givenLockedSession(readyForManualCall().build());

		sendCallService.send(command());

		InOrder inOrder = inOrder(saveCallPort, sessionEventPort);
		inOrder.verify(saveCallPort).saveCall(any(Call.class));
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_TRIGGERED"), eq(STATE_VERSION), eq(NOW), any());
		SessionEvent appended = appendedEventOf("CALL_TRIGGERED");
		assertThat(appended.getPayload())
				.containsEntry("callId", NEW_CALL_ID)
				.containsEntry("trigger", "MANUAL");
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("CALL_TRIGGERED");
	}

	@Test
	@DisplayName("합류 안내 보내기가 성공하면 저장한 CALL_TRIGGERED를 append가 준 eventId · seq로 보호자에게 발행한다")
	void sendPublishesCallTriggeredToGuardianAfterAppending() {
		givenLockedSession(readyForManualCall().build());

		sendCallService.send(command());

		InOrder inOrder = inOrder(sessionEventPort, publishSessionEventPort);
		inOrder.verify(sessionEventPort).append(anyLong(), eq("CALL_TRIGGERED"), anyInt(), any(), any());
		inOrder.verify(publishSessionEventPort)
				.publish(argThat(message -> "CALL_TRIGGERED".equals(message.type())));
		SessionEvent appended = appendedEventOf("CALL_TRIGGERED");
		SessionEventMessage message = publishedMessageOf("CALL_TRIGGERED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.payload()).isEqualTo(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	private void givenJoinCallMode(JoinCallMode mode) {
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(new FamilyInfo(FAMILY_ID, mode, 0));
	}

	// 세션 행 잠금 안에서 처리하므로 loadForUpdate만 준비한다
	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void givenLiveCall(Call call) {
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(call));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call));
	}

	private Call savedCall() {
		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
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

	// 가드 하나만 통과하지 못하게 만든 세션
	private static PickupSession breaking(GuardId guard) {
		SessionBuilder builder = readyForManualCall();
		return switch (guard) {
			case G1 -> builder.sessionStatus(SessionStatus.ON_HOLD).build();
			case G2 -> builder.spotStatus(SpotStatus.SELECTED).build();
			case G3 -> builder.routeSpotVersion(SPOT_VERSION - 1).build();
			case G4 -> builder.etaUpdatedAt(NOW.minusSeconds(61)).build();
			case G5 -> builder.seoyunCallNotBefore(NOW.plusSeconds(60)).build();
		};
	}

	// 가드 G1~G5를 모두 통과하고, ETA 10분 · 남은 거리 4.2km라 자동 호출 트리거는 맞지 않는 세션
	private static SessionBuilder readyForManualCall() {
		return new SessionBuilder();
	}

	private static Call liveCall(CallStatus status) {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(2));
		Instant sentAt = status == CallStatus.ACCEPTED ? null : acceptedAt.plusSeconds(1);
		Instant deliveredAt = status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED
				? acceptedAt.plusSeconds(3) : null;
		Instant acknowledgedAt = status == CallStatus.ACKNOWLEDGED ? acceptedAt.plusSeconds(20) : null;
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.MANUAL, status, SPOT_VERSION,
				acceptedAt, sentAt, deliveredAt, acknowledgedAt, null, null);
	}

	private static Call revokedCall() {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(5));
		return Call.restore(REVOKED_CALL_ID, SESSION_ID, null, CallTriggerType.MANUAL, CallStatus.REVOKED,
				SPOT_VERSION, acceptedAt, acceptedAt.plusSeconds(1), acceptedAt.plusSeconds(3), null,
				acceptedAt.plusSeconds(60), "아이가 아직 준비 중이라 철회");
	}

	private static SendCallCommand command() {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new SendCallCommand(vehicle, SESSION_ID, STATE_VERSION);
	}

	private static final class SessionBuilder {

		private SessionStatus sessionStatus = SessionStatus.ACTIVE;
		private SpotStatus spotStatus = SpotStatus.CONFIRMED;
		private Integer routeSpotVersion = SPOT_VERSION;
		private Instant etaUpdatedAt = NOW.minusSeconds(10);
		private Instant seoyunCallNotBefore;

		SessionBuilder sessionStatus(SessionStatus value) {
			this.sessionStatus = value;
			return this;
		}

		SessionBuilder spotStatus(SpotStatus value) {
			this.spotStatus = value;
			return this;
		}

		SessionBuilder routeSpotVersion(Integer value) {
			this.routeSpotVersion = value;
			return this;
		}

		SessionBuilder etaUpdatedAt(Instant value) {
			this.etaUpdatedAt = value;
			return this;
		}

		SessionBuilder seoyunCallNotBefore(Instant value) {
			this.seoyunCallNotBefore = value;
			return this;
		}

		PickupSession build() {
			List<SessionChild> children = List.of(
					SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null,
							null, null, null),
					SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING,
							seoyunCallNotBefore == null ? 0 : 5, seoyunCallNotBefore, null, null, null, null));
			return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
					FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, sessionStatus, VehiclePhase.APPROACHING,
					spotStatus, STATE_VERSION, SPOT_VERSION, ROUTE_VERSION, routeSpotVersion, false,
					NOW.plus(Duration.ofMinutes(10)), 4200, etaUpdatedAt, null, null, null, null, null, null, null, null,
					children);
		}

	}

}

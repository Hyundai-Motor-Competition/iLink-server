package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AcknowledgeCallServiceTest {

	// 한국 시간 2026-11-09(월) 18:10
	private static final Instant NOW = Instant.parse("2026-11-09T09:10:00Z");
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
	private static final long CALL_ID = 3301L;

	// 18:09 합류 안내가 두 자녀 폰에 도착(DELIVERED)해 둘 다 CALLED인 세션
	private static final int STATE_VERSION = 11;
	private static final int SPOT_VERSION = 1;
	private static final long LAST_SEQ = 9L;
	private static final long EVENT_ID_BASE = 9000L;
	private static final Instant SENT_AT = Instant.parse("2026-11-09T09:09:01Z");
	private static final Instant DELIVERED_AT = Instant.parse("2026-11-09T09:09:03Z");
	private static final Instant MINJUN_ACKNOWLEDGED_AT = Instant.parse("2026-11-09T09:09:30Z");
	private static final Instant REVOKED_AT = Instant.parse("2026-11-09T09:09:40Z");

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private SaveCallPort saveCallPort;

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	private AcknowledgeCallService acknowledgeCallService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		acknowledgeCallService = new AcknowledgeCallService(loadCallPort, saveCallPort, loadSessionPort,
				saveSessionPort, loadFamilyPort, sessionEventPort, publishSessionEventPort, clock);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		Call deliveredCall = deliveredCall();
		lenient().when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.of(deliveredCall));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(deliveredCall));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(deliveredCall));
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(saveCallPort.saveCall(any(Call.class)))
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

	@Test
	@DisplayName("호출 확인은 확인한 자녀만 MEETING · acknowledgedAt 지금으로 바꾸고 stateVersion 1 올려 세션을 저장한다")
	void acknowledgeSavesOnlyThatChildAsMeetingAndRaisesStateVersion() {
		givenLockedSession(session(List.of(calledChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				calledChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID))));

		acknowledgeCallService.acknowledge(command(MINJUN_ID));

		PickupSession saved = savedSession();
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		SessionChild minjun = childOf(saved, MINJUN_ID);
		assertThat(minjun.getChildPhase()).isEqualTo(ChildPhase.MEETING);
		assertThat(minjun.getAcknowledgedAt()).isEqualTo(NOW);
		SessionChild seoyun = childOf(saved, SEOYUN_ID);
		assertThat(seoyun.getChildPhase()).isEqualTo(ChildPhase.CALLED);
		assertThat(seoyun.getAcknowledgedAt()).isNull();
	}

	@Test
	@DisplayName("호출 확인은 형제 중 아직 확인하지 않은 자녀가 있으면 Call을 ACKNOWLEDGED로 바꾸지 않고 CALL_ACKNOWLEDGED를 저장 · 발행하지 않는다")
	void acknowledgeBySomeSiblingsDoesNotAppendCallAcknowledged() {
		givenLockedSession(session(List.of(calledChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				calledChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID))));

		acknowledgeCallService.acknowledge(command(MINJUN_ID));

		verify(saveCallPort, never()).saveCall(argThat(call -> call.getStatus() == CallStatus.ACKNOWLEDGED));
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verifyNoInteractions(publishSessionEventPort);
	}

	@Test
	@DisplayName("호출 확인은 마지막 형제가 확인하면 Call을 ACKNOWLEDGED · acknowledgedAt 지금으로 저장한다")
	void acknowledgeByLastSiblingSavesCallAcknowledged() {
		givenLockedSession(session(List.of(acknowledgedChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				calledChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID))));

		acknowledgeCallService.acknowledge(command(SEOYUN_ID));

		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort, atLeastOnce()).saveCall(captor.capture());
		Call saved = captor.getValue();
		assertThat(saved.getCallId()).isEqualTo(CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACKNOWLEDGED);
		assertThat(saved.getAcknowledgedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("호출 확인은 마지막 형제가 확인하면 CALL_ACKNOWLEDGED를 저장한 세션의 stateVersion · 지금 시각으로 한 번만 저장한다")
	void acknowledgeByLastSiblingAppendsCallAcknowledgedOnce() {
		givenLockedSession(session(List.of(acknowledgedChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				calledChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID))));

		acknowledgeCallService.acknowledge(command(SEOYUN_ID));

		assertThat(savedSession().getStateVersion()).isEqualTo(STATE_VERSION + 1);
		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_ACKNOWLEDGED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("CALL_ACKNOWLEDGED");
	}

	@Test
	@DisplayName("호출 확인은 저장한 CALL_ACKNOWLEDGED를 append가 준 eventId · seq로 보호자에게 발행한다")
	void acknowledgeByLastSiblingPublishesCallAcknowledgedToGuardian() {
		givenLockedSession(session(List.of(acknowledgedChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				calledChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID))));

		acknowledgeCallService.acknowledge(command(SEOYUN_ID));

		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages).extracting(SessionEventMessage::type).containsExactly("CALL_ACKNOWLEDGED");
		SessionEventMessage message = messages.get(0);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.payload()).isEqualTo(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("호출 확인은 형제가 없는 한 자녀 세션이면 그 자녀가 확인하자마자 CALL_ACKNOWLEDGED를 저장한다")
	void acknowledgeInSingleChildSessionAppendsCallAcknowledged() {
		givenLockedSession(session(List.of(calledChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID))));

		acknowledgeCallService.acknowledge(command(MINJUN_ID));

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_ACKNOWLEDGED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("CALL_ACKNOWLEDGED");
	}

	@Test
	@DisplayName("철회된 호출을 확인하면 409 CALL_ALREADY_REVOKED로 거절하고 자녀 상태를 바꾸지 않으며 세션 · 호출 · 이벤트를 저장하지 않는다")
	void acknowledgeRevokedCallIsRejectedWithCallAlreadyRevokedAndSavesNothing() {
		// 18:09:40 차량이 철회해 두 자녀가 WAITING으로 돌아간 세션
		PickupSession revokedSession = session(List.of(waitingChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				waitingChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID)));
		givenLockedSession(revokedSession);
		lenient().when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.of(revokedCall()));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(revokedCall()));

		assertThatThrownBy(() -> acknowledgeCallService.acknowledge(command(MINJUN_ID)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(exception.code()).isEqualTo("CALL_ALREADY_REVOKED");
				});

		assertThat(revokedSession.getStateVersion()).isEqualTo(STATE_VERSION);
		SessionChild minjun = childOf(revokedSession, MINJUN_ID);
		assertThat(minjun.getChildPhase()).isEqualTo(ChildPhase.WAITING);
		assertThat(minjun.getAcknowledgedAt()).isNull();
		verify(saveSessionPort, never()).save(any());
		verify(saveCallPort, never()).saveCall(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verifyNoInteractions(publishSessionEventPort);
	}

	@Test
	@DisplayName("호출 확인은 세션 행을 잠근 뒤 Call을 다시 읽어, 잠금을 기다리는 사이 철회됐으면 409 CALL_ALREADY_REVOKED로 거절하고 자녀를 MEETING으로 바꾸지 않으며 세션 · 호출 · 이벤트를 저장 · 발행하지 않는다")
	void acknowledgeRereadsCallAfterLockAndRejectsCallRevokedMeanwhile() {
		// 민준이 DELIVERED 호출을 읽은 뒤 잠금을 기다리는 사이 18:09:40 차량이 철회해 두 자녀가 WAITING으로 돌아감
		PickupSession revokedSession = session(List.of(waitingChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				waitingChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID)));
		givenLockedSession(revokedSession);
		Call revokedMeanwhile = revokedCall();
		lenient().when(loadCallPort.loadCall(CALL_ID))
				.thenReturn(Optional.of(deliveredCall()), Optional.of(revokedMeanwhile));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(revokedMeanwhile));

		assertThatThrownBy(() -> acknowledgeCallService.acknowledge(command(MINJUN_ID)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(exception.code()).isEqualTo("CALL_ALREADY_REVOKED");
				});

		assertThat(revokedSession.getStateVersion()).isEqualTo(STATE_VERSION);
		SessionChild minjun = childOf(revokedSession, MINJUN_ID);
		assertThat(minjun.getChildPhase()).isEqualTo(ChildPhase.WAITING);
		assertThat(minjun.getAcknowledgedAt()).isNull();
		assertThat(revokedMeanwhile.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(revokedMeanwhile.getAcknowledgedAt()).isNull();
		verify(saveSessionPort, never()).save(any());
		verify(saveCallPort, never()).saveCall(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verifyNoInteractions(publishSessionEventPort);
		InOrder inOrder = inOrder(loadCallPort, loadSessionPort);
		inOrder.verify(loadCallPort).loadCall(CALL_ID);
		inOrder.verify(loadSessionPort).loadForUpdate(SESSION_ID);
		inOrder.verify(loadCallPort).loadCall(CALL_ID);
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort, atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}

	private List<SessionEventMessage> publishedMessages() {
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		return captor.getAllValues();
	}

	private static SessionChild childOf(PickupSession session, long childUserId) {
		return session.getChildren().stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.findFirst()
				.orElseThrow();
	}

	// 호출이 철회돼 안전 대기로 돌아간 자녀
	private static SessionChild waitingChild(long sessionChildId, long childUserId, long scheduleId) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.WAITING, 0, null, null, null,
				null, null);
	}

	private static SessionChild calledChild(long sessionChildId, long childUserId, long scheduleId) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.CALLED, 0, null, null, null,
				null, null);
	}

	// 먼저 「확인했어요」를 눌러 차로 이동 중인 형제
	private static SessionChild acknowledgedChild(long sessionChildId, long childUserId, long scheduleId) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.MEETING, 0, null, null, null,
				MINJUN_ACKNOWLEDGED_AT, null);
	}

	private static PickupSession session(List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, 2, SPOT_VERSION, false,
				NOW.plus(Duration.ofMinutes(2)), 500, NOW.minusSeconds(5), null, null, null, null, null, null, null,
				children);
	}

	private static Call deliveredCall() {
		Instant acceptedAt = SENT_AT.minusSeconds(1);
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.DELIVERED, SPOT_VERSION,
				acceptedAt, SENT_AT, DELIVERED_AT, null, null, null);
	}

	private static Call revokedCall() {
		Instant acceptedAt = SENT_AT.minusSeconds(1);
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.REVOKED, SPOT_VERSION,
				acceptedAt, SENT_AT, DELIVERED_AT, null, REVOKED_AT, "장소를 바꿉니다");
	}

	private static AcknowledgeCallCommand command(long childUserId) {
		AuthPrincipal child = new AuthPrincipal(PrincipalType.USER, childUserId, FAMILY_ID, Role.CHILD);
		return new AcknowledgeCallCommand(child, CALL_ID);
	}

}

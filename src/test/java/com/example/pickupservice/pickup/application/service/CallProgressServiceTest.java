package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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

@ExtendWith(MockitoExtension.class)
class CallProgressServiceTest {

	// 한국 시간 2026-11-09(월) 18:09:00에 호출을 만들고, 18:09:02에 FCM 발송, 18:09:05에 자녀 폰 수신 확인
	private static final Instant ACCEPTED_AT = Instant.parse("2026-11-09T09:09:00Z");
	private static final Instant SENT_AT = ACCEPTED_AT.plusSeconds(2);
	private static final Instant DELIVERED_AT = SENT_AT.plusSeconds(3);
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

	// 설계서 SSE 예시처럼 발송 전 stateVersion 15 → CALL_SENT는 stateVersion 16 · seq 42 · eventId 9001
	private static final int STATE_VERSION = 15;
	private static final int STATE_VERSION_AFTER_SENT = STATE_VERSION + 1;
	private static final int SPOT_VERSION = 1;
	private static final int ROUTE_VERSION = 2;
	private static final long LAST_SEQ = 41L;
	private static final long LAST_EVENT_ID = 9000L;

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

	private CallProgressService callProgressService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final AtomicLong nextEventId = new AtomicLong(LAST_EVENT_ID);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		callProgressService = new CallProgressService(loadCallPort, saveCallPort, loadSessionPort, saveSessionPort,
				loadFamilyPort, sessionEventPort, publishSessionEventPort);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(saveCallPort.saveCall(any(Call.class))).thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(sessionEventPort.append(anyLong(), anyString(), anyInt(), any(Instant.class), any()))
				.thenAnswer(invocation -> {
					SessionEvent event = SessionEvent.restore(nextEventId.incrementAndGet(),
							invocation.<Long>getArgument(0), nextSeq.incrementAndGet(),
							SessionEventType.valueOf(invocation.<String>getArgument(1)),
							invocation.<Integer>getArgument(2), invocation.<Instant>getArgument(3),
							invocation.getArgument(4));
					appendedEvents.add(event);
					return event;
				});
	}

	// ── markCallSent ──

	@Test
	@DisplayName("markCallSent는 ACCEPTED Call을 SENT로 바꾸고 sentAt에 받은 발송 시각을 채워 한 번 저장한다")
	void markCallSentSavesCallAsSentWithSentAt() {
		givenCall(acceptedCall());
		givenLockedSession(session(STATE_VERSION, ChildPhase.WAITING));

		callProgressService.markCallSent(CALL_ID, SENT_AT);

		Call saved = savedCall();
		assertThat(saved.getCallId()).isEqualTo(CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.SENT);
		assertThat(saved.getSentAt()).isEqualTo(SENT_AT);
		assertThat(saved.getAcceptedAt()).isEqualTo(ACCEPTED_AT);
	}

	@Test
	@DisplayName("markCallSent는 세션 행 잠금으로 읽은 세션의 자녀를 모두 CALLED로 바꾸고 stateVersion을 1 올려 저장한다")
	void markCallSentMarksSessionChildrenCalledAndRaisesStateVersion() {
		givenCall(acceptedCall());
		givenLockedSession(session(STATE_VERSION, ChildPhase.WAITING));

		callProgressService.markCallSent(CALL_ID, SENT_AT);

		PickupSession saved = savedSession();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase)
				.containsExactlyInAnyOrder(tuple(MINJUN_ID, ChildPhase.CALLED), tuple(SEOYUN_ID, ChildPhase.CALLED));
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION_AFTER_SENT);
	}

	@Test
	@DisplayName("markCallSent는 CALL_SENT를 올린 stateVersion · 발송 시각과 payload callId · trigger로 한 번 저장한다")
	void markCallSentAppendsCallSentWithRaisedStateVersion() {
		givenCall(acceptedCall());
		givenLockedSession(session(STATE_VERSION, ChildPhase.WAITING));

		callProgressService.markCallSent(CALL_ID, SENT_AT);

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_SENT"), eq(STATE_VERSION_AFTER_SENT), eq(SENT_AT), any());
		SessionEvent appended = appendedEventOf("CALL_SENT");
		assertThat(appended.getPayload())
				.containsEntry("callId", CALL_ID)
				.containsEntry("trigger", "ETA_3MIN");
	}

	@Test
	@DisplayName("markCallSent는 저장한 CALL_SENT를 append가 준 eventId · seq로 보호자에게(차량은 SSE) 발행한다")
	void markCallSentPublishesCallSentToGuardian() {
		givenCall(acceptedCall());
		givenLockedSession(session(STATE_VERSION, ChildPhase.WAITING));

		callProgressService.markCallSent(CALL_ID, SENT_AT);

		SessionEvent appended = appendedEventOf("CALL_SENT");
		SessionEventMessage message = publishedMessageOf("CALL_SENT");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(9001L);
		assertThat(message.seq()).isEqualTo(42L);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION_AFTER_SENT);
		assertThat(message.occurredAt()).isEqualTo(SENT_AT);
		assertThat(message.payload()).isEqualTo(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("철회된 호출에 늦게 온 markCallSent는 무시해 Call · 세션 · CALL_SENT를 저장 · 발행하지 않고 Call은 REVOKED · sentAt 없음, 자녀는 WAITING · stateVersion 그대로다")
	void markCallSentIgnoresRevokedCall() {
		Call revoked = revokedCall(null);
		PickupSession session = session(STATE_VERSION_AFTER_SENT, ChildPhase.WAITING);
		givenCall(revoked);
		givenLockedSession(session);

		callProgressService.markCallSent(CALL_ID, SENT_AT);

		assertNothingSavedOrPublished();
		assertThat(revoked.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(revoked.getSentAt()).isNull();
		assertThat(session.getChildren()).extracting(SessionChild::getChildPhase).containsOnly(ChildPhase.WAITING);
		assertThat(session.getStateVersion()).isEqualTo(STATE_VERSION_AFTER_SENT);
	}

	// ── markCallDelivered ──

	@Test
	@DisplayName("markCallDelivered는 SENT Call을 DELIVERED로 바꾸고 deliveredAt에 받은 수신 확인 시각을 채워 한 번 저장한다(sentAt은 그대로)")
	void markCallDeliveredSavesCallAsDeliveredWithDeliveredAt() {
		givenCall(sentCall());
		givenLockedSession(session(STATE_VERSION_AFTER_SENT, ChildPhase.CALLED));

		callProgressService.markCallDelivered(CALL_ID, DELIVERED_AT);

		Call saved = savedCall();
		assertThat(saved.getCallId()).isEqualTo(CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.DELIVERED);
		assertThat(saved.getDeliveredAt()).isEqualTo(DELIVERED_AT);
		assertThat(saved.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("markCallDelivered는 자녀 상태가 바뀌지 않아 stateVersion을 올리지 않고 CALL_DELIVERED를 지금 stateVersion · 수신 확인 시각과 payload callId로 한 번 저장한다")
	void markCallDeliveredAppendsCallDeliveredWithCurrentStateVersion() {
		PickupSession session = session(STATE_VERSION_AFTER_SENT, ChildPhase.CALLED);
		givenCall(sentCall());
		givenLockedSession(session);

		callProgressService.markCallDelivered(CALL_ID, DELIVERED_AT);

		verify(sessionEventPort).append(eq(SESSION_ID), eq("CALL_DELIVERED"), eq(STATE_VERSION_AFTER_SENT),
				eq(DELIVERED_AT), any());
		assertThat(appendedEventOf("CALL_DELIVERED").getPayload()).containsEntry("callId", CALL_ID);
		assertThat(session.getStateVersion()).isEqualTo(STATE_VERSION_AFTER_SENT);
		assertThat(session.getChildren()).extracting(SessionChild::getChildPhase).containsOnly(ChildPhase.CALLED);
	}

	@Test
	@DisplayName("markCallDelivered는 저장한 CALL_DELIVERED를 append가 준 eventId · seq로 보호자에게(차량은 SSE) 발행한다")
	void markCallDeliveredPublishesCallDeliveredToGuardian() {
		givenCall(sentCall());
		givenLockedSession(session(STATE_VERSION_AFTER_SENT, ChildPhase.CALLED));

		callProgressService.markCallDelivered(CALL_ID, DELIVERED_AT);

		SessionEvent appended = appendedEventOf("CALL_DELIVERED");
		SessionEventMessage message = publishedMessageOf("CALL_DELIVERED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION_AFTER_SENT);
		assertThat(message.occurredAt()).isEqualTo(DELIVERED_AT);
		assertThat(message.payload()).isEqualTo(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("철회된 호출에 늦게 온 markCallDelivered는 무시해 Call · 세션 · CALL_DELIVERED를 저장 · 발행하지 않고 Call은 REVOKED · deliveredAt 없음 그대로다")
	void markCallDeliveredIgnoresRevokedCall() {
		Call revoked = revokedCall(SENT_AT);
		givenCall(revoked);
		givenLockedSession(session(STATE_VERSION_AFTER_SENT + 1, ChildPhase.WAITING));

		callProgressService.markCallDelivered(CALL_ID, DELIVERED_AT);

		assertNothingSavedOrPublished();
		assertThat(revoked.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(revoked.getDeliveredAt()).isNull();
	}

	// ── 잠금을 기다리는 사이 철회된 호출 ──

	@Test
	@DisplayName("markCallSent는 세션 행을 잠근 뒤 Call을 다시 읽어, 잠금을 기다리는 사이 철회됐으면 Call · 세션 · CALL_SENT를 저장 · 발행하지 않고 자녀를 CALLED로 되돌리지 않는다")
	void markCallSentRereadsCallAfterLockAndIgnoresCallRevokedMeanwhile() {
		Call revokedMeanwhile = revokedCall(null);
		PickupSession session = session(STATE_VERSION_AFTER_SENT, ChildPhase.WAITING);
		givenCallRevokedWhileWaitingForLock(acceptedCall(), revokedMeanwhile);
		givenLockedSession(session);

		callProgressService.markCallSent(CALL_ID, SENT_AT);

		assertNothingSavedOrPublished();
		assertThat(session.getChildren()).extracting(SessionChild::getChildPhase).containsOnly(ChildPhase.WAITING);
		assertThat(session.getStateVersion()).isEqualTo(STATE_VERSION_AFTER_SENT);
		assertThat(revokedMeanwhile.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(revokedMeanwhile.getSentAt()).isNull();
		assertCallRereadAfterLock();
	}

	@Test
	@DisplayName("markCallDelivered는 세션 행을 잠근 뒤 Call을 다시 읽어, 잠금을 기다리는 사이 철회됐으면 Call · CALL_DELIVERED를 저장 · 발행하지 않는다")
	void markCallDeliveredRereadsCallAfterLockAndIgnoresCallRevokedMeanwhile() {
		Call revokedMeanwhile = revokedCall(SENT_AT);
		givenCallRevokedWhileWaitingForLock(sentCall(), revokedMeanwhile);
		givenLockedSession(session(STATE_VERSION_AFTER_SENT + 1, ChildPhase.WAITING));

		callProgressService.markCallDelivered(CALL_ID, DELIVERED_AT);

		assertNothingSavedOrPublished();
		assertThat(revokedMeanwhile.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(revokedMeanwhile.getDeliveredAt()).isNull();
		assertCallRereadAfterLock();
	}

	// ── isCallAcknowledged ──

	@Test
	@DisplayName("isCallAcknowledged는 Call이 ACKNOWLEDGED면 true를 돌려준다")
	void isCallAcknowledgedReturnsTrueForAcknowledgedCall() {
		givenCall(callWithStatus(CallStatus.ACKNOWLEDGED));

		assertThat(callProgressService.isCallAcknowledged(CALL_ID)).isTrue();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = CallStatus.class, names = "ACKNOWLEDGED", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("isCallAcknowledged는 Call이 ACKNOWLEDGED가 아니면(ACCEPTED · SENT · DELIVERED · REVOKED) false를 돌려준다")
	void isCallAcknowledgedReturnsFalseForNotAcknowledgedCall(CallStatus status) {
		givenCall(callWithStatus(status));

		assertThat(callProgressService.isCallAcknowledged(CALL_ID)).isFalse();
	}

	@Test
	@DisplayName("isCallAcknowledged는 없는 callId면 확인된 호출이 아니므로 예외 없이 false를 돌려준다")
	void isCallAcknowledgedReturnsFalseForUnknownCall() {
		lenient().when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.empty());

		assertThat(callProgressService.isCallAcknowledged(CALL_ID)).isFalse();
	}

	// ── 준비 ──

	private void givenCall(Call call) {
		boolean live = call.getStatus() != CallStatus.REVOKED;
		lenient().when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.of(call));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(live ? Optional.of(call) : Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call));
	}

	// 처음 읽을 땐 살아 있던 호출이, 세션 행 잠금을 기다리는 사이 다른 요청(CALL-03)이 철회해 다시 읽으면 REVOKED
	private void givenCallRevokedWhileWaitingForLock(Call beforeLock, Call afterLock) {
		lenient().when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.of(beforeLock), Optional.of(afterLock));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(afterLock));
	}

	// Call을 바꾸는 요청도 CALL-04(확인) · CALL-03(철회)와 겹칠 수 있어 세션 행 잠금(loadForUpdate)으로 읽는다
	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private static Call acceptedCall() {
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.ACCEPTED, SPOT_VERSION,
				ACCEPTED_AT, null, null, null, null, null);
	}

	private static Call sentCall() {
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.SENT, SPOT_VERSION,
				ACCEPTED_AT, SENT_AT, null, null, null, null);
	}

	// 보호자가 철회한 뒤에 발송 · 수신 확인 결과가 늦게 도착한 호출
	private static Call revokedCall(Instant sentAt) {
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.REVOKED, SPOT_VERSION,
				ACCEPTED_AT, sentAt, null, null, ACCEPTED_AT.plusSeconds(1), "아이가 아직 준비 중");
	}

	// 상태에 맞는 시각만 채운 호출(isCallAcknowledged 판정용)
	private static Call callWithStatus(CallStatus status) {
		Instant sentAt = status == CallStatus.ACCEPTED ? null : SENT_AT;
		Instant deliveredAt = status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED ? DELIVERED_AT : null;
		Instant acknowledgedAt = status == CallStatus.ACKNOWLEDGED ? DELIVERED_AT.plusSeconds(20) : null;
		Instant revokedAt = status == CallStatus.REVOKED ? SENT_AT.plusSeconds(10) : null;
		String revokeReason = status == CallStatus.REVOKED ? "아이가 아직 준비 중" : null;
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, SPOT_VERSION, ACCEPTED_AT,
				sentAt, deliveredAt, acknowledgedAt, revokedAt, revokeReason);
	}

	private static PickupSession session(int stateVersion, ChildPhase childPhase) {
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, childPhase, 0, null, null, null, null,
						null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, childPhase, 0, null, null, null, null,
						null));
		Instant etaUpdatedAt = ACCEPTED_AT.minusSeconds(2);
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, stateVersion, SPOT_VERSION, ROUTE_VERSION, SPOT_VERSION, false,
				etaUpdatedAt.plusSeconds(170), 1500, etaUpdatedAt, null, null, null, null, null, null, null,
				children);
	}

	// ── 확인 ──

	private void assertNothingSavedOrPublished() {
		verify(saveCallPort, never()).saveCall(any());
		verify(saveSessionPort, never()).save(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
	}

	// Call 읽기 → 세션 행 잠금 → Call 다시 읽기 순서
	private void assertCallRereadAfterLock() {
		InOrder inOrder = inOrder(loadCallPort, loadSessionPort);
		inOrder.verify(loadCallPort).loadCall(CALL_ID);
		inOrder.verify(loadSessionPort).loadForUpdate(SESSION_ID);
		inOrder.verify(loadCallPort).loadCall(CALL_ID);
	}

	private Call savedCall() {
		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		return captor.getValue();
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

}

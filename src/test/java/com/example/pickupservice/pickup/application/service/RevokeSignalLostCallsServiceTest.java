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
import com.example.pickupservice.pickup.domain.BoardingDoor;
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
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevokeSignalLostCallsServiceTest {

	// 한국 시간 2026-11-09(월) 18:30:00. ETA 유효시간 1분이므로 etaUpdatedAt이 18:29:00보다 앞이면 신호 끊김
	private static final Instant NOW = Instant.parse("2026-11-09T09:30:00Z");
	private static final Duration ETA_VALID_FOR = Duration.ofMinutes(1);
	private static final Instant STALE_BEFORE = NOW.minus(ETA_VALID_FOR);
	private static final Instant STALE_ETA_UPDATED_AT = NOW.minusSeconds(61);
	private static final Instant FRESH_ETA_UPDATED_AT = NOW.minusSeconds(30);
	private static final Instant EXACTLY_ONE_MINUTE_AGO = NOW.minusSeconds(60);
	private static final Instant CALL_SENT_AT = NOW.minusSeconds(200);
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

	// 같은 시각에 신호가 끊긴 다른 세션(예외가 나도 다음 세션을 처리하는지 볼 때 쓴다)
	private static final long NEXT_SESSION_ID = 502L;
	private static final long NEXT_VEHICLE_ID = 78L;
	private static final long NEXT_FAMILY_ID = 13L;
	private static final long NEXT_GUARDIAN_ID = 41L;
	private static final long JIHO_ID = 43L;
	private static final long JIWOO_ID = 44L;
	private static final long JIHO_SCHEDULE_ID = 811L;
	private static final long JIWOO_SCHEDULE_ID = 812L;
	private static final long NEXT_CALL_ID = 3002L;

	private static final int STATE_VERSION = 8;
	private static final long LAST_SEQ = 7L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng CHILD_LOCATION = new LatLng(37.501350, 127.039900);

	private static final PickupProperties PROPERTIES = new PickupProperties(ETA_VALID_FOR, Duration.ofSeconds(180),
			Duration.ofSeconds(10), 700, Duration.ofSeconds(60), Duration.ofMinutes(15), 90);

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

	private RevokeSignalLostCallsService revokeSignalLostCallsService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		revokeSignalLostCallsService = new RevokeSignalLostCallsService(loadSessionPort, saveSessionPort, loadCallPort,
				saveCallPort, loadFamilyPort, sessionEventPort, publishSessionEventPort, PROPERTIES,
				TransactionOperations.withoutTransaction());
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(loadFamilyPort.loadMembers(NEXT_FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(NEXT_GUARDIAN_ID, NEXT_FAMILY_ID, Role.GUARDIAN, "정보호", "010-9999-0000"),
				new MemberInfo(JIHO_ID, NEXT_FAMILY_ID, Role.CHILD, "지호", null),
				new MemberInfo(JIWOO_ID, NEXT_FAMILY_ID, Role.CHILD, "지우", null)));
		lenient().when(saveCallPort.saveCall(any(Call.class))).thenAnswer(invocation -> invocation.getArgument(0));
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

	@ParameterizedTest(name = "호출 {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("신호 끊김 철회는 etaUpdatedAt이 1분 넘게 지난 ACTIVE 세션의 철회되지 않은 호출을 REVOKED · revokedAt 지금 · revokeReason SIGNAL_LOST로 바꿔 저장하고 1을 돌려준다")
	void revokesLiveCallOfSignalLostSessionWithSignalLostReason(CallStatus liveStatus) {
		givenSignalLost(staleSession());
		givenLocked(staleSession());
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, liveStatus));

		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isEqualTo(1);
		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		Call saved = captor.getValue();
		assertThat(saved.getCallId()).isEqualTo(LIVE_CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(saved.getRevokedAt()).isEqualTo(NOW);
		assertThat(saved.getRevokeReason()).isEqualTo("SIGNAL_LOST");
	}

	@Test
	@DisplayName("신호 끊김 철회는 지금에서 ETA 유효시간(1분)을 뺀 시각을 기준으로 신호 끊김 세션을 조회한다")
	void loadsSignalLostSessionsWithEtaValidTimeBeforeNow() {
		revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		verify(loadSessionPort).loadSignalLost(STALE_BEFORE);
	}

	@Test
	@DisplayName("신호 끊김 철회로 호출을 철회하면 세션 자녀를 모두 WAITING으로 돌리고 자녀 위치를 비우며 stateVersion을 올려 저장한다")
	void revertsChildrenToWaitingClearsLocationAndRaisesStateVersion() {
		givenSignalLost(staleSession());
		givenLocked(staleSession());
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, CallStatus.SENT));

		revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		PickupSession saved = savedSession();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(saved.getChildren()).hasSize(2).allSatisfy(child -> {
			assertThat(child.getChildPhase()).isEqualTo(ChildPhase.WAITING);
			assertThat(child.getLastLocation()).isNull();
			assertThat(child.getLastLocatedAt()).isNull();
		});
	}

	@Test
	@DisplayName("신호 끊김 철회는 CALL_REVOKED를 저장한 세션의 stateVersion · 지금 시각으로 저장하고 보호자와 세션 자녀에게 발행한다")
	void appendsAndPublishesCallRevokedToGuardiansAndSessionChildren() {
		givenSignalLost(staleSession());
		givenLocked(staleSession());
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, CallStatus.DELIVERED));

		revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_REVOKED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents).extracting(event -> event.getType().name()).containsExactly("CALL_REVOKED");
		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages).hasSize(1);
		SessionEventMessage message = messages.get(0);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.type()).isEqualTo("CALL_REVOKED");
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("신호 끊김 철회는 조회된 세션이 없으면 아무것도 저장 · 발행하지 않고 0을 돌려준다")
	void doesNothingWhenNoSessionIsSignalLost() {
		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isZero();
		verifyNothingRevoked();
	}

	@Test
	@DisplayName("신호 끊김 철회는 잠가 다시 읽은 세션의 etaUpdatedAt이 그 사이 1분 안으로 갱신됐으면 호출을 철회하지 않는다")
	void skipsSessionWhoseEtaWasRefreshedBeforeLock() {
		givenSignalLost(staleSession());
		givenLocked(sessionWithEtaUpdatedAt(SESSION_ID, FRESH_ETA_UPDATED_AT));
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, CallStatus.SENT));

		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isZero();
		verify(loadSessionPort).loadForUpdate(SESSION_ID);
		verifyNothingRevoked();
	}

	@Test
	@DisplayName("신호 끊김 철회는 etaUpdatedAt이 정확히 1분 전이면 1분 넘게 지난 것이 아니므로 호출을 철회하지 않는다")
	void skipsSessionWhoseEtaIsExactlyOneMinuteOld() {
		givenSignalLost(sessionWithEtaUpdatedAt(SESSION_ID, EXACTLY_ONE_MINUTE_AGO));
		givenLocked(sessionWithEtaUpdatedAt(SESSION_ID, EXACTLY_ONE_MINUTE_AGO));
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, CallStatus.SENT));

		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isZero();
		verifyNothingRevoked();
	}

	@Test
	@DisplayName("신호 끊김 철회는 잠가 다시 읽은 세션에 철회되지 않은 호출이 없으면(그 사이 철회됨) 호출 저장 · 세션 저장 · CALL_REVOKED 없이 0을 돌려준다")
	void skipsSessionWithoutLiveCallAfterLock() {
		givenSignalLost(staleSession());
		givenLocked(staleSession());
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());

		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isZero();
		verifyNothingRevoked();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = SessionStatus.class, names = {"ON_HOLD", "COMPLETED", "CANCELLED"})
	@DisplayName("신호 끊김 철회는 잠가 다시 읽은 세션이 ACTIVE가 아니면(그 사이 보류 · 완료 · 취소) 호출을 철회하지 않는다")
	void skipsSessionThatIsNotActiveAfterLock(SessionStatus notActive) {
		givenSignalLost(staleSession());
		givenLocked(session(SESSION_ID, VEHICLE_ID, FAMILY_ID, notActive, STALE_ETA_UPDATED_AT, locatedCalledChildren()));
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, CallStatus.SENT));

		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isZero();
		verifyNothingRevoked();
	}

	@Test
	@DisplayName("신호 끊김 철회는 잠가 다시 읽은 ACTIVE 세션에 boardingDetectedAt이 있으면(탑승 인식) 호출을 철회하지 않고 자녀 상태 · acknowledgedAt을 바꾸지 않으며 0을 돌려준다")
	void skipsBoardingDetectedSessionAfterLock() {
		PickupSession boarded = boardingDetectedStaleSession();
		givenSignalLost(staleSession());
		givenLocked(boarded);
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, CallStatus.ACKNOWLEDGED));

		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isZero();
		verifyNothingRevoked();
		assertThat(boarded.getStateVersion()).isEqualTo(STATE_VERSION);
		assertThat(boarded.getChildren()).hasSize(2).allSatisfy(child -> {
			assertThat(child.getChildPhase()).isEqualTo(ChildPhase.MEETING);
			assertThat(child.getAcknowledgedAt()).isEqualTo(CALL_SENT_AT.plusSeconds(20));
			assertThat(child.getLastLocation()).isEqualTo(CHILD_LOCATION);
		});
	}

	@Test
	@DisplayName("신호 끊김 철회는 한 세션을 처리하다 예외가 나도 다음 세션의 호출을 철회 · CALL_REVOKED 저장 · 발행하고 철회한 세션 수 1을 돌려준다")
	void keepsRevokingNextSessionWhenOneSessionFails() {
		PickupSession failing = staleSession();
		PickupSession next = nextFamilyStaleSession();
		lenient().when(loadSessionPort.loadSignalLost(STALE_BEFORE)).thenReturn(List.of(failing, next));
		givenLocked(staleSession());
		lenient().when(loadSessionPort.loadForUpdate(NEXT_SESSION_ID)).thenReturn(Optional.of(nextFamilyStaleSession()));
		givenLiveCall(SESSION_ID, call(LIVE_CALL_ID, SESSION_ID, CallStatus.SENT));
		givenLiveCall(NEXT_SESSION_ID, call(NEXT_CALL_ID, NEXT_SESSION_ID, CallStatus.SENT));
		lenient().when(saveCallPort.saveCall(any(Call.class))).thenAnswer(invocation -> {
			Call call = invocation.getArgument(0);
			if (call.getSessionId() == SESSION_ID) {
				throw new IllegalStateException("저장 실패");
			}
			return call;
		});

		int revoked = revokeSignalLostCallsService.revokeSignalLostCalls(NOW);

		assertThat(revoked).isEqualTo(1);
		ArgumentCaptor<Call> callCaptor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort, atLeastOnce()).saveCall(callCaptor.capture());
		assertThat(callCaptor.getAllValues())
				.filteredOn(call -> call.getSessionId() == NEXT_SESSION_ID)
				.singleElement()
				.satisfies(call -> {
					assertThat(call.getStatus()).isEqualTo(CallStatus.REVOKED);
					assertThat(call.getRevokeReason()).isEqualTo("SIGNAL_LOST");
				});
		verify(sessionEventPort).append(eq(NEXT_SESSION_ID), eq("CALL_REVOKED"), anyInt(), eq(NOW), any());
		assertThat(publishedMessages()).extracting(SessionEventMessage::sessionId).containsExactly(NEXT_SESSION_ID);
	}

	private void givenSignalLost(PickupSession... sessions) {
		lenient().when(loadSessionPort.loadSignalLost(STALE_BEFORE)).thenReturn(List.of(sessions));
	}

	private void givenLocked(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(session.getSessionId())).thenReturn(Optional.of(session));
	}

	private void givenLiveCall(long sessionId, Call call) {
		lenient().when(loadCallPort.loadLiveCall(sessionId)).thenReturn(Optional.of(call));
	}

	private void verifyNothingRevoked() {
		verify(saveCallPort, never()).saveCall(any(Call.class));
		verify(saveSessionPort, never()).save(any(PickupSession.class));
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(Instant.class), any());
		verify(publishSessionEventPort, never()).publish(any(SessionEventMessage.class));
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

	// 합류 안내를 보낸 뒤 차량 신호가 61초 전에 끊긴 ACTIVE 세션
	private static PickupSession staleSession() {
		return sessionWithEtaUpdatedAt(SESSION_ID, STALE_ETA_UPDATED_AT);
	}

	private static PickupSession sessionWithEtaUpdatedAt(long sessionId, Instant etaUpdatedAt) {
		return session(sessionId, VEHICLE_ID, FAMILY_ID, SessionStatus.ACTIVE, etaUpdatedAt, locatedCalledChildren());
	}

	// 신호가 끊겼지만 문 열림을 인식해(boardingDetectedAt 있음) 취소 창 안이라 아직 ACTIVE인 세션. 두 자녀는 합류 안내를 확인해 MEETING
	private static PickupSession boardingDetectedStaleSession() {
		Instant boardedAt = STALE_ETA_UPDATED_AT.minusSeconds(5);
		Instant acknowledgedAt = CALL_SENT_AT.plusSeconds(20);
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞", STOP_POINT,
				MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.STOPPED, SpotStatus.CONFIRMED,
				STATE_VERSION, 0, 1, 0, false, STALE_ETA_UPDATED_AT.plusSeconds(40), 300, STALE_ETA_UPDATED_AT,
				boardedAt, boardedAt.plusSeconds(10), BoardingDoor.REAR_RIGHT,
				null, null, null, null, null,
				List.of(
						SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null,
								CHILD_LOCATION, NOW.minusSeconds(70), acknowledgedAt, boardedAt),
						SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null,
								CHILD_LOCATION, NOW.minusSeconds(70), acknowledgedAt, boardedAt)));
	}

	private static PickupSession nextFamilyStaleSession() {
		return session(NEXT_SESSION_ID, NEXT_VEHICLE_ID, NEXT_FAMILY_ID, SessionStatus.ACTIVE, STALE_ETA_UPDATED_AT,
				List.of(
						SessionChild.restore(7101L, JIHO_ID, JIHO_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
								CHILD_LOCATION, NOW.minusSeconds(70), null, null),
						SessionChild.restore(7102L, JIWOO_ID, JIWOO_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
								CHILD_LOCATION, NOW.minusSeconds(70), null, null)));
	}

	private static PickupSession session(long sessionId, long vehicleId, long familyId, SessionStatus status,
			Instant etaUpdatedAt, List<SessionChild> children) {
		return PickupSession.restore(sessionId, vehicleId, familyId, FRONT_GATE_SPOT_ID, "정문 앞", STOP_POINT,
				MEETING_POINT, TODAY_KST, status, VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, STATE_VERSION, 0, 1,
				0, false, etaUpdatedAt.plusSeconds(40), 300, etaUpdatedAt, null, null, null, null, null, null, null,
				null, children);
	}

	private static List<SessionChild> locatedCalledChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, CHILD_LOCATION,
						NOW.minusSeconds(70), null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, CHILD_LOCATION,
						NOW.minusSeconds(70), null, null));
	}

	private static Call call(long callId, long sessionId, CallStatus status) {
		Instant sentAt = status == CallStatus.ACCEPTED ? null : CALL_SENT_AT;
		Instant deliveredAt = status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED
				? CALL_SENT_AT.plusSeconds(1) : null;
		Instant acknowledgedAt = status == CallStatus.ACKNOWLEDGED ? CALL_SENT_AT.plusSeconds(20) : null;
		return Call.restore(callId, sessionId, null, CallTriggerType.ETA_3MIN, status, 0, CALL_SENT_AT.minusSeconds(1),
				sentAt, deliveredAt, acknowledgedAt, null, null);
	}

}

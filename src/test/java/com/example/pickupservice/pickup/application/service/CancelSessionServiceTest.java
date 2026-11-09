package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CancelSessionCommand;
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
class CancelSessionServiceTest {

	// 한국 시간 2026-11-09(월) 17:50
	private static final Instant NOW = Instant.parse("2026-11-09T08:50:00Z");
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
	private static final String REASON = "아이가 아파서 오늘 픽업을 취소합니다";

	// 합류 안내(SENT)를 보내 두 자녀가 CALLED이고 위치를 보내는 중인 세션
	private static final int STATE_VERSION = 8;
	private static final long LAST_SEQ = 7L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng MINJUN_LOCATION = new LatLng(37.501500, 127.039700);
	private static final LatLng SEOYUN_LOCATION = new LatLng(37.501550, 127.039650);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private SaveCallPort saveCallPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	private CancelSessionService cancelSessionService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, clock);
		cancelSessionService = new CancelSessionService(loadSessionPort, saveSessionPort, loadFamilyPort,
				loadCallPort, saveCallPort, sessionEventPort, publishSessionEventPort, sessionAccess, clock);
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
	@DisplayName("픽업 취소는 철회되지 않은 호출을 REVOKED · revokedAt 지금 · revokeReason 취소 사유로 저장한다")
	void cancelRevokesLiveCallWithReason() {
		givenLockedSession(calledSessionWithLocations());
		givenLiveCall(sentCall());

		cancelSessionService.cancel(command());

		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		Call saved = captor.getValue();
		assertThat(saved.getCallId()).isEqualTo(LIVE_CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(saved.getRevokedAt()).isEqualTo(NOW);
		assertThat(saved.getRevokeReason()).isEqualTo(REASON);
	}

	@Test
	@DisplayName("픽업 취소는 CALL_REVOKED 다음에 SESSION_CANCELLED를 지금 시각으로 한 번씩 저장하고 SESSION_CANCELLED는 저장한 세션의 stateVersion을 갖는다")
	void cancelAppendsCallRevokedThenSessionCancelled() {
		givenLockedSession(calledSessionWithLocations());
		givenLiveCall(sentCall());

		cancelSessionService.cancel(command());

		PickupSession saved = savedSession();
		assertThat(saved.getStateVersion()).isGreaterThan(STATE_VERSION);
		InOrder inOrder = inOrder(sessionEventPort);
		inOrder.verify(sessionEventPort).append(eq(SESSION_ID), eq("CALL_REVOKED"), anyInt(), eq(NOW), any());
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SESSION_CANCELLED"), eq(saved.getStateVersion()), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("CALL_REVOKED", "SESSION_CANCELLED");
	}

	@Test
	@DisplayName("픽업 취소는 저장한 CALL_REVOKED와 SESSION_CANCELLED를 그 순서 · append가 준 seq로 보호자와 세션 자녀에게 발행한다")
	void cancelPublishesCallRevokedThenSessionCancelledToGuardianAndChildren() {
		givenLockedSession(calledSessionWithLocations());
		givenLiveCall(sentCall());

		cancelSessionService.cancel(command());

		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages)
				.extracting(SessionEventMessage::type)
				.containsExactly("CALL_REVOKED", "SESSION_CANCELLED");
		for (int i = 0; i < messages.size(); i++) {
			SessionEventMessage message = messages.get(i);
			SessionEvent appended = appendedEvents.get(i);
			assertThat(message.sessionId()).isEqualTo(SESSION_ID);
			assertThat(message.eventId()).isEqualTo(appended.getEventId());
			assertThat(message.seq()).isEqualTo(appended.getSeq());
			assertThat(message.stateVersion()).isEqualTo(appended.getStateVersion());
			assertThat(message.occurredAt()).isEqualTo(NOW);
			assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
		}
	}

	@Test
	@DisplayName("픽업 취소는 철회되지 않은 호출이 없으면 호출을 저장하지 않고 CALL_REVOKED 없이 SESSION_CANCELLED만 저장한다")
	void cancelWithoutLiveCallAppendsOnlySessionCancelled() {
		givenLockedSession(waitingSession());

		cancelSessionService.cancel(command());

		verify(saveCallPort, never()).saveCall(any());
		verify(sessionEventPort, never()).append(anyLong(), eq("CALL_REVOKED"), anyInt(), any(), any());
		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SESSION_CANCELLED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("SESSION_CANCELLED");
	}

	@Test
	@DisplayName("픽업 취소는 세션을 CANCELLED로 저장하면서 모든 자녀의 lastLat · lastLng · lastLocatedAt을 비운다")
	void cancelClearsChildLocations() {
		givenLockedSession(calledSessionWithLocations());
		givenLiveCall(sentCall());

		cancelSessionService.cancel(command());

		PickupSession saved = savedSession();
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.CANCELLED);
		assertThat(saved.getChildren()).hasSize(2);
		assertThat(saved.getChildren())
				.allSatisfy(child -> {
					assertThat(child.getLastLocation()).as("lastLat · lastLng").isNull();
					assertThat(child.getLastLocatedAt()).as("lastLocatedAt").isNull();
				});
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void givenLiveCall(Call call) {
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(call));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call));
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

	private static PickupSession calledSessionWithLocations() {
		Instant locatedAt = NOW.minusSeconds(5);
		return session(List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						MINJUN_LOCATION, locatedAt, null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						SEOYUN_LOCATION, locatedAt, null, null)));
	}

	private static PickupSession waitingSession() {
		return session(List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null)));
	}

	private static PickupSession session(List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, NOW.plus(Duration.ofMinutes(2)), 600,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, children);
	}

	private static Call sentCall() {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(2));
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.SENT, 0,
				acceptedAt, acceptedAt.plusSeconds(1), null, null, null, null);
	}

	private static CancelSessionCommand command() {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
		return new CancelSessionCommand(guardian, SESSION_ID, STATE_VERSION, REASON);
	}

}

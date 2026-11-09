package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.RevokeCallCommand;
import com.example.pickupservice.pickup.application.port.in.SessionChildView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RevokeCallServiceTest {

	// 한국 시간 2026-11-09(월) 18:11
	private static final Instant NOW = Instant.parse("2026-11-09T09:11:00Z");
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
	private static final String REASON = "정문 앞에 공사 차량이 있어 잠시 기다려 주세요";

	// 18:09 합류 안내(SENT)를 보냈고 민준은 확인해 이동 중(MEETING), 서윤은 아직 CALLED. 둘 다 위치를 보내는 중
	private static final int STATE_VERSION = 12;
	private static final int SPOT_VERSION = 1;
	private static final long LAST_SEQ = 10L;
	private static final long EVENT_ID_BASE = 9000L;
	private static final Instant SENT_AT = Instant.parse("2026-11-09T09:09:01Z");
	private static final Instant MINJUN_ACKNOWLEDGED_AT = Instant.parse("2026-11-09T09:09:30Z");
	// 18:10:58 다른 요청이 먼저 철회
	private static final Instant EARLIER_REVOKED_AT = NOW.minusSeconds(2);
	private static final String EARLIER_REVOKE_REASON = "아이가 아직 준비 중";

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng MINJUN_LOCATION = new LatLng(37.501500, 127.039700);
	private static final LatLng SEOYUN_LOCATION = new LatLng(37.501550, 127.039650);

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

	private RevokeCallService revokeCallService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, clock);
		revokeCallService = new RevokeCallService(loadCallPort, saveCallPort, loadSessionPort, saveSessionPort,
				loadFamilyPort, sessionEventPort, publishSessionEventPort, sessionAccess, clock);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		Call sentCall = sentCall();
		lenient().when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.of(sentCall));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(sentCall));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(sentCall));
		PickupSession session = calledSessionWithLocations();
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
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
	@DisplayName("호출 철회는 CALLED · MEETING 세션 자녀를 모두 WAITING으로 되돌리고 lastLat · lastLng · lastLocatedAt · acknowledgedAt을 비워 stateVersion 1 올려 저장한다")
	void revokeRevertsSessionChildrenToWaitingAndClearsLocations() {
		revokeCallService.revoke(command());

		PickupSession saved = savedSession();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(saved.getChildren()).hasSize(2);
		assertThat(saved.getChildren())
				.allSatisfy(child -> {
					assertThat(child.getChildPhase()).as("childPhase").isEqualTo(ChildPhase.WAITING);
					assertThat(child.getLastLocation()).as("lastLat · lastLng").isNull();
					assertThat(child.getLastLocatedAt()).as("lastLocatedAt").isNull();
					assertThat(child.getAcknowledgedAt()).as("acknowledgedAt").isNull();
				});
	}

	@Test
	@DisplayName("호출 철회는 그 호출을 REVOKED · revokedAt 지금 · revokeReason 요청 사유로 저장한다")
	void revokeSavesCallRevokedWithReason() {
		revokeCallService.revoke(command());

		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		Call saved = captor.getValue();
		assertThat(saved.getCallId()).isEqualTo(CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(saved.getRevokedAt()).isEqualTo(NOW);
		assertThat(saved.getRevokeReason()).isEqualTo(REASON);
	}

	@Test
	@DisplayName("호출 철회는 CALL_REVOKED를 저장한 세션의 stateVersion · 지금 시각으로 한 번만 저장한다")
	void revokeAppendsCallRevokedOnceWithSavedStateVersion() {
		revokeCallService.revoke(command());

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_REVOKED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("CALL_REVOKED");
	}

	@Test
	@DisplayName("호출 철회는 저장한 CALL_REVOKED를 append가 준 eventId · seq로 보호자와 세션 자녀에게 발행한다")
	void revokePublishesCallRevokedToGuardianAndSessionChildren() {
		revokeCallService.revoke(command());

		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages).extracting(SessionEventMessage::type).containsExactly("CALL_REVOKED");
		SessionEventMessage message = messages.get(0);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.payload()).isEqualTo(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("호출 철회는 올린 stateVersion과 WAITING 자녀를 담은 세션 뷰를 돌려준다")
	void revokeReturnsSessionViewWithWaitingChildren() {
		SessionView view = revokeCallService.revoke(command());

		assertThat(view.sessionId()).isEqualTo(SESSION_ID);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.children())
				.extracting(SessionChildView::phase)
				.containsOnly(ChildPhase.WAITING);
	}

	// 처음 읽은 SENT 호출을, 세션 행 잠금을 기다리는 사이 다른 요청이 먼저 철회한 상황.
	// stateVersion 검사를 통과하더라도 잠근 뒤 다시 읽은 Call 상태로 거절해야 한다
	@Test
	@DisplayName("호출 철회는 세션 행을 잠근 뒤 Call을 다시 읽어, 잠금을 기다리는 사이 이미 철회됐으면 409 CALL_ALREADY_REVOKED로 거절하고 revokedAt · revokeReason을 덮어쓰지 않으며 세션 · 호출 · 이벤트를 저장 · 발행하지 않는다")
	void revokeRereadsCallAfterLockAndRejectsCallRevokedMeanwhile() {
		Call revokedMeanwhile = revokedCall();
		lenient().when(loadCallPort.loadCall(CALL_ID))
				.thenReturn(Optional.of(sentCall()), Optional.of(revokedMeanwhile));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(revokedMeanwhile));

		assertThatThrownBy(() -> revokeCallService.revoke(command()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(exception.code()).isEqualTo("CALL_ALREADY_REVOKED");
				});

		assertThat(revokedMeanwhile.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(revokedMeanwhile.getRevokedAt()).isEqualTo(EARLIER_REVOKED_AT);
		assertThat(revokedMeanwhile.getRevokeReason()).isEqualTo(EARLIER_REVOKE_REASON);
		verify(saveCallPort, never()).saveCall(any());
		verify(saveSessionPort, never()).save(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verifyNoInteractions(publishSessionEventPort);
		InOrder inOrder = inOrder(loadCallPort, loadSessionPort);
		inOrder.verify(loadCallPort).loadCall(CALL_ID);
		inOrder.verify(loadSessionPort).loadForUpdate(SESSION_ID);
		inOrder.verify(loadCallPort).loadCall(CALL_ID);
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
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null,
						MINJUN_LOCATION, locatedAt, MINJUN_ACKNOWLEDGED_AT, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						SEOYUN_LOCATION, locatedAt, null, null));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, 2, SPOT_VERSION, false,
				NOW.plus(Duration.ofMinutes(1)), 300, NOW.minusSeconds(10), null, null, null, null, null, null, null,
				children);
	}

	private static Call sentCall() {
		Instant acceptedAt = SENT_AT.minusSeconds(1);
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.SENT, SPOT_VERSION,
				acceptedAt, SENT_AT, null, null, null, null);
	}

	// 다른 요청이 먼저 철회한 호출
	private static Call revokedCall() {
		Instant acceptedAt = SENT_AT.minusSeconds(1);
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.REVOKED, SPOT_VERSION,
				acceptedAt, SENT_AT, null, null, EARLIER_REVOKED_AT, EARLIER_REVOKE_REASON);
	}

	private static RevokeCallCommand command() {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
		return new RevokeCallCommand(guardian, CALL_ID, STATE_VERSION, REASON);
	}

}

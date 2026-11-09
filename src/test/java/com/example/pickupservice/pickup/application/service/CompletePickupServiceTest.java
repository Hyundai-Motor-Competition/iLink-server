package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CompletePickupCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveOutcomePort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupOutcome;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompletePickupServiceTest {

	// 한국 시간 2026-11-09(월) 18:13:25 — 18:13:20 뒷문 탑승 인식 뒤 취소 창(10초) 안
	private static final Instant NOW = Instant.parse("2026-11-09T09:13:25Z");
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
	private static final long CALL_ID = 3301L;

	private static final int STATE_VERSION = 15;
	private static final int SPOT_VERSION = 1;
	private static final long LAST_SEQ = 14L;
	private static final long EVENT_ID_BASE = 9000L;

	// 정차 직전 마지막 ETA 18:12:00, 실제 정차 18:12:40(SIG-02 STOPPED occurredAt)
	// DB에서 읽은 payload는 JSON이라 시각이 ISO-8601 문자열로 온다
	private static final String PREDICTED_STOP_AT_JSON = "2026-11-09T18:12:00+09:00";
	private static final Instant PREDICTED_STOP_AT = Instant.parse("2026-11-09T09:12:00Z");
	private static final Instant STOPPED_AT = Instant.parse("2026-11-09T09:12:40Z");
	private static final int STATE_VERSION_AT_STOP = 13;
	private static final long VEHICLE_STOPPED_SEQ = 12L;
	private static final Instant ACKNOWLEDGED_AT = Instant.parse("2026-11-09T09:09:30Z");
	private static final Instant DOOR_AT = Instant.parse("2026-11-09T09:13:20Z");
	private static final Instant LOCATED_AT = Instant.parse("2026-11-09T09:13:10Z");

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng MINJUN_LOCATION = new LatLng(37.501350, 127.039920);
	private static final LatLng SEOYUN_LOCATION = new LatLng(37.501360, 127.039930);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private SaveOutcomePort saveOutcomePort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	private CompletePickupService completePickupService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, ASSEMBLER, clock);
		PickupCompletion pickupCompletion = new PickupCompletion(saveOutcomePort, sessionEventPort,
				publishSessionEventPort);
		completePickupService = new CompletePickupService(loadSessionPort, saveSessionPort, loadFamilyPort,
				loadCallPort, sessionAccess, pickupCompletion, clock);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		Call acknowledgedCall = acknowledgedCall();
		lenient().when(loadCallPort.loadCall(CALL_ID)).thenReturn(Optional.of(acknowledgedCall));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(acknowledgedCall));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(acknowledgedCall));
		lenient().when(sessionEventPort.loadFirst(SESSION_ID, "VEHICLE_STOPPED"))
				.thenReturn(Optional.of(vehicleStopped(Map.of("predictedStopAt", PREDICTED_STOP_AT_JSON))));
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(saveOutcomePort.saveOutcome(any(PickupOutcome.class)))
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
	@DisplayName("탑승 완료 IMMEDIATE는 PickupOutcome을 sessionId · boardingMethod IMMEDIATE · completedAt 지금으로 한 번 저장한다")
	void completeImmediateSavesOutcomeWithImmediateMethodAndCompletedAtNow() {
		givenLockedSession(doorBoardedSession());

		completePickupService.complete(command("IMMEDIATE", List.of()));

		PickupOutcome outcome = savedOutcome();
		assertThat(outcome.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(outcome.getBoardingMethod()).isEqualTo(BoardingMethod.IMMEDIATE);
		assertThat(outcome.getCompletedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("탑승 완료 IMMEDIATE는 세션을 COMPLETED · stateVersion 1 올리고 자녀 위치를 비워 저장한다")
	void completeImmediateSavesSessionCompletedWithRaisedStateVersion() {
		givenLockedSession(doorBoardedSession());

		completePickupService.complete(command("IMMEDIATE", List.of()));

		PickupSession saved = savedSession();
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(saved.getChildren()).allSatisfy(child -> {
			assertThat(child.getLastLocation()).isNull();
			assertThat(child.getLastLocatedAt()).isNull();
		});
	}

	@Test
	@DisplayName("탑승 완료 IMMEDIATE는 SESSION_COMPLETED를 저장한 세션의 stateVersion · 지금 시각으로 한 번만 저장한다")
	void completeImmediateAppendsSessionCompletedOnce() {
		givenLockedSession(doorBoardedSession());

		completePickupService.complete(command("IMMEDIATE", List.of()));

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SESSION_COMPLETED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("SESSION_COMPLETED");
	}

	@Test
	@DisplayName("탑승 완료는 저장한 SESSION_COMPLETED를 append가 준 eventId · seq로 보호자와 세션 자녀 모두에게 발행한다")
	void completePublishesSessionCompletedToGuardianAndSessionChildren() {
		givenLockedSession(doorBoardedSession());

		completePickupService.complete(command("IMMEDIATE", List.of()));

		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages).extracting(SessionEventMessage::type).containsExactly("SESSION_COMPLETED");
		SessionEventMessage message = messages.get(0);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("탑승 완료 MANUAL은 미탑승 지정 외 자녀의 boardedAt을 지금으로 채우고 미탑승 지정 자녀는 boardedAt 없이 NOT_BOARDED로 저장한다")
	void completeManualBoardsChildrenExceptNotBoarded() {
		givenLockedSession(stoppedSessionWithoutDoorSignal());

		completePickupService.complete(command("MANUAL", List.of(SEOYUN_ID)));

		PickupSession saved = savedSession();
		SessionChild minjun = childOf(saved, MINJUN_ID);
		assertThat(minjun.getBoardedAt()).isEqualTo(NOW);
		SessionChild seoyun = childOf(saved, SEOYUN_ID);
		assertThat(seoyun.getBoardedAt()).isNull();
		assertThat(seoyun.getChildPhase()).isEqualTo(ChildPhase.NOT_BOARDED);
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
	}

	@Test
	@DisplayName("탑승 완료 MANUAL은 PickupOutcome을 boardingMethod MANUAL · completedAt 지금으로 저장한다")
	void completeManualSavesOutcomeWithManualMethod() {
		givenLockedSession(stoppedSessionWithoutDoorSignal());

		completePickupService.complete(command("MANUAL", List.of(SEOYUN_ID)));

		PickupOutcome outcome = savedOutcome();
		assertThat(outcome.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(outcome.getBoardingMethod()).isEqualTo(BoardingMethod.MANUAL);
		assertThat(outcome.getCompletedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("탑승 완료는 PickupOutcome.actualStopAt에 첫 VEHICLE_STOPPED의 occurredAt을, predictedStopAt에 그 payload의 predictedStopAt을 넣는다")
	void completeSavesActualStopAtFromVehicleStoppedOccurredAt() {
		givenLockedSession(doorBoardedSession());

		completePickupService.complete(command("IMMEDIATE", List.of()));

		verify(sessionEventPort, atLeastOnce()).loadFirst(SESSION_ID, "VEHICLE_STOPPED");
		PickupOutcome outcome = savedOutcome();
		assertThat(outcome.getActualStopAt()).isEqualTo(STOPPED_AT);
		assertThat(outcome.getPredictedStopAt()).isEqualTo(PREDICTED_STOP_AT);
	}

	@Test
	@DisplayName("탑승 완료는 VEHICLE_STOPPED payload의 predictedStopAt이 +09:00 문자열(18:12:00+09:00)이어도 같은 시각(UTC 09:12:00)으로 읽어 PickupOutcome.predictedStopAt에 저장한다")
	void completeReadsKstOffsetPredictedStopAtFromPayload() {
		givenLockedSession(doorBoardedSession());
		lenient().when(sessionEventPort.loadFirst(SESSION_ID, "VEHICLE_STOPPED"))
				.thenReturn(Optional.of(vehicleStopped(Map.of("predictedStopAt", "2026-11-09T18:12:00+09:00"))));

		completePickupService.complete(command("IMMEDIATE", List.of()));

		assertThat(savedOutcome().getPredictedStopAt()).isEqualTo(Instant.parse("2026-11-09T09:12:00Z"));
	}

	@Test
	@DisplayName("탑승 완료는 VEHICLE_STOPPED payload의 predictedStopAt이 비어 있으면(정차 때 ETA 없음) predictedStopAt은 비우고 actualStopAt은 occurredAt으로 저장한다")
	void completeWithoutPredictedStopAtInPayloadSavesOnlyActualStopAt() {
		givenLockedSession(doorBoardedSession());
		Map<String, Object> payload = new HashMap<>();
		payload.put("predictedStopAt", null);
		lenient().when(sessionEventPort.loadFirst(SESSION_ID, "VEHICLE_STOPPED"))
				.thenReturn(Optional.of(vehicleStopped(payload)));

		completePickupService.complete(command("IMMEDIATE", List.of()));

		PickupOutcome outcome = savedOutcome();
		assertThat(outcome.getActualStopAt()).isEqualTo(STOPPED_AT);
		assertThat(outcome.getPredictedStopAt()).isNull();
	}

	@Test
	@DisplayName("탑승 완료는 VEHICLE_STOPPED가 없으면(정차 전 완료) actualStopAt · predictedStopAt을 비워 저장한다")
	void completeWithoutVehicleStoppedSavesNullStopTimes() {
		givenLockedSession(approachingSessionNobodyBoarded());
		lenient().when(sessionEventPort.loadFirst(SESSION_ID, "VEHICLE_STOPPED")).thenReturn(Optional.empty());

		completePickupService.complete(command("IMMEDIATE", List.of(MINJUN_ID, SEOYUN_ID)));

		PickupOutcome outcome = savedOutcome();
		assertThat(outcome.getBoardingMethod()).isEqualTo(BoardingMethod.IMMEDIATE);
		assertThat(outcome.getActualStopAt()).isNull();
		assertThat(outcome.getPredictedStopAt()).isNull();
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private PickupOutcome savedOutcome() {
		ArgumentCaptor<PickupOutcome> captor = ArgumentCaptor.forClass(PickupOutcome.class);
		verify(saveOutcomePort).saveOutcome(captor.capture());
		return captor.getValue();
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

	// 18:13:20 뒷문 열림으로 두 자녀 모두 탑승 인식된 세션(도어 인식 차량)
	private static PickupSession doorBoardedSession() {
		return session(VehiclePhase.STOPPED, DOOR_AT, BoardingDoor.REAR_RIGHT, List.of(
				meetingChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, MINJUN_LOCATION, DOOR_AT),
				meetingChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, SEOYUN_LOCATION, DOOR_AT)));
	}

	// 도어 신호 없는 차량이 정차했고 두 자녀가 차로 오는 중인 세션
	private static PickupSession stoppedSessionWithoutDoorSignal() {
		return session(VehiclePhase.STOPPED, null, null, List.of(
				meetingChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, MINJUN_LOCATION, null),
				meetingChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, SEOYUN_LOCATION, null)));
	}

	// 정차 전, 아무도 타지 않은 세션(두 자녀 모두 미탑승 지정으로 완료)
	private static PickupSession approachingSessionNobodyBoarded() {
		return session(VehiclePhase.APPROACHING, null, null, List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null)));
	}

	private static SessionChild meetingChild(long sessionChildId, long childUserId, long scheduleId,
			LatLng location, Instant boardedAt) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.MEETING, 0, null, location,
				LOCATED_AT, ACKNOWLEDGED_AT, boardedAt);
	}

	private static PickupSession session(VehiclePhase vehiclePhase, Instant boardingDetectedAt, BoardingDoor door,
			List<SessionChild> children) {
		Instant cancelableUntil = boardingDetectedAt == null ? null : boardingDetectedAt.plusSeconds(10);
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, vehiclePhase,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, 2, SPOT_VERSION, false, PREDICTED_STOP_AT, 0,
				STOPPED_AT, boardingDetectedAt, cancelableUntil, door, null, null, null, null, null, children);
	}

	private static SessionEvent vehicleStopped(Map<String, Object> payload) {
		return SessionEvent.restore(EVENT_ID_BASE + VEHICLE_STOPPED_SEQ, SESSION_ID, VEHICLE_STOPPED_SEQ,
				SessionEventType.VEHICLE_STOPPED, STATE_VERSION_AT_STOP, STOPPED_AT, payload);
	}

	private static Call acknowledgedCall() {
		Instant sentAt = Instant.parse("2026-11-09T09:09:01Z");
		return Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.ACKNOWLEDGED,
				SPOT_VERSION, sentAt.minusSeconds(1), sentAt, sentAt.plusSeconds(2), ACKNOWLEDGED_AT, null, null);
	}

	private static CompletePickupCommand command(String mode, List<Long> notBoardedChildUserIds) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new CompletePickupCommand(vehicle, SESSION_ID, mode, STATE_VERSION, notBoardedChildUserIds);
	}

}

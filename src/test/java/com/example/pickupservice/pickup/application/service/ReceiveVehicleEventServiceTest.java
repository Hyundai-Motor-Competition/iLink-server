package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.VehicleEventCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.ProcessedVehicleEventPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.BoardingMethod;
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
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiveVehicleEventServiceTest {

	// 한국 시간 2026-11-09(월) 17:50
	private static final Instant NOW = Instant.parse("2026-11-09T08:50:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	// 정차 직전 마지막 ETA 17:51:30, 실제 정차 17:51:58, 문 열림 17:53:10, 출발 17:54:00
	private static final Instant ETA_UPDATED_AT = Instant.parse("2026-11-09T08:51:20Z");
	private static final Instant PREDICTED_STOP_AT = Instant.parse("2026-11-09T08:51:30Z");
	private static final Instant STOPPED_AT = Instant.parse("2026-11-09T08:51:58Z");
	private static final Instant DOOR_AT = Instant.parse("2026-11-09T08:53:10Z");
	private static final Instant DEPARTED_AT = Instant.parse("2026-11-09T08:54:00Z");
	private static final Duration CANCEL_WINDOW = Duration.ofSeconds(10);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long OTHER_VEHICLE_ID = 78L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long LIVE_CALL_ID = 3001L;
	private static final String EVENT_ID = "6f1c2b9e-3d4a-4e8b-9a71-0c5d2e8f4b13";

	private static final int STATE_VERSION = 8;
	private static final long LAST_SEQ = 7L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng VEHICLE_LOCATION = new LatLng(37.501402, 127.039948);

	private static final PickupProperties PROPERTIES = new PickupProperties(Duration.ofMinutes(1),
			Duration.ofSeconds(180), CANCEL_WINDOW, 700, Duration.ofSeconds(60), Duration.ofMinutes(15), 90);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private ProcessedVehicleEventPort processedVehicleEventPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	private ReceiveVehicleEventService receiveVehicleEventService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		receiveVehicleEventService = new ReceiveVehicleEventService(loadSessionPort, saveSessionPort, loadCallPort,
				loadFamilyPort, processedVehicleEventPort, sessionEventPort, publishSessionEventPort, PROPERTIES);
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(processedVehicleEventPort.markProcessed(anyLong(), anyString())).thenReturn(true);
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.empty());
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

	// #104 eventId 중복

	@Test
	@DisplayName("차량 이벤트 수신은 같은 eventId를 두 번째로 받으면 세션을 바꾸거나 저장하지 않고 이벤트를 저장 · 발행하지 않는다")
	void receiveIgnoresSecondEventWithSameEventId() {
		givenLockedSession(approachingSession(PREDICTED_STOP_AT));
		when(processedVehicleEventPort.markProcessed(SESSION_ID, EVENT_ID)).thenReturn(true, false);
		VehicleEventCommand stopped = command("STOPPED", STOPPED_AT, null);

		receiveVehicleEventService.receive(stopped);
		receiveVehicleEventService.receive(stopped);

		verify(processedVehicleEventPort, times(2)).markProcessed(SESSION_ID, EVENT_ID);
		verify(saveSessionPort, times(1)).save(any());
		verify(sessionEventPort, times(1)).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, times(1)).publish(any());
		assertThat(savedSession().getStateVersion()).isEqualTo(STATE_VERSION + 1);
	}

	@Test
	@DisplayName("다른 차량이 보낸 차량 이벤트는 NOT_FOUND로 거절하고 eventId를 처리한 것으로 남기지 않는다")
	void receiveRejectsOtherVehicleWithNotFoundBeforeMarkingEventId() {
		givenLockedSession(approachingSession(PREDICTED_STOP_AT));
		AuthPrincipal otherVehicle = new AuthPrincipal(PrincipalType.VEHICLE, OTHER_VEHICLE_ID, FAMILY_ID, null);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> receiveVehicleEventService.receive(new VehicleEventCommand(otherVehicle, SESSION_ID, EVENT_ID,
						"STOPPED", STOPPED_AT, VEHICLE_LOCATION, null)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verify(processedVehicleEventPort, never()).markProcessed(anyLong(), anyString());
		verify(saveSessionPort, never()).save(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
	}

	// #105 STOPPED

	@Test
	@DisplayName("차량 이벤트 STOPPED는 eventId를 처리한 것으로 남기고 세션 vehiclePhase를 STOPPED로 바꿔 stateVersion +1로 저장한다")
	void stoppedMarksVehiclePhaseStopped() {
		givenLockedSession(approachingSession(PREDICTED_STOP_AT));

		receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null));

		verify(processedVehicleEventPort).markProcessed(SESSION_ID, EVENT_ID);
		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.STOPPED);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
	}

	@Test
	@DisplayName("차량 이벤트 STOPPED는 VEHICLE_STOPPED를 저장한 세션의 stateVersion과 실제 정차 시각 occurredAt으로 한 번 저장한다")
	void stoppedAppendsVehicleStoppedAtOccurredAt() {
		givenLockedSession(approachingSession(PREDICTED_STOP_AT));

		receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null));

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("VEHICLE_STOPPED"), eq(STATE_VERSION + 1), eq(STOPPED_AT), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_STOPPED");
	}

	@Test
	@DisplayName("VEHICLE_STOPPED payload의 predictedStopAt은 STOPPED를 받을 때의 세션 etaAt(정차 직전 마지막 ETA)이다")
	void stoppedPayloadCarriesSessionEtaAsPredictedStopAt() {
		givenLockedSession(approachingSession(PREDICTED_STOP_AT));

		receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null));

		assertThat(appendedEvents).hasSize(1);
		assertThat(appendedEvents.get(0).getPayload()).containsEntry("predictedStopAt", PREDICTED_STOP_AT);
	}

	@Test
	@DisplayName("세션 etaAt이 없으면 VEHICLE_STOPPED payload의 predictedStopAt은 null이다")
	void stoppedPayloadPredictedStopAtIsNullWithoutEta() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.ROUTING, null, calledChildren()));

		receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null));

		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_STOPPED");
		Map<String, Object> payload = appendedEvents.get(0).getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload.get("predictedStopAt")).isNull();
	}

	@Test
	@DisplayName("차량 이벤트 STOPPED는 저장한 VEHICLE_STOPPED를 append가 준 eventId · seq · payload로 보호자와 세션 자녀에게 발행한다")
	void stoppedPublishesVehicleStoppedToGuardianAndChildren() {
		givenLockedSession(approachingSession(PREDICTED_STOP_AT));

		receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null));

		assertPublishedOnce("VEHICLE_STOPPED", STOPPED_AT, GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	// #106 DEPARTED

	@Test
	@DisplayName("차량 이벤트 DEPARTED는 세션 vehiclePhase를 DEPARTED로 바꿔 저장하고 VEHICLE_DEPARTED를 저장한 stateVersion · occurredAt으로 한 번 저장한다")
	void departedMarksVehiclePhaseDepartedAndAppendsVehicleDeparted() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT, calledChildren()));

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.DEPARTED);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("VEHICLE_DEPARTED"), eq(STATE_VERSION + 1), eq(DEPARTED_AT), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_DEPARTED");
	}

	@Test
	@DisplayName("차량 이벤트 DEPARTED는 저장한 VEHICLE_DEPARTED를 append가 준 eventId · seq · payload로 보호자와 세션 자녀에게 발행한다")
	void departedPublishesVehicleDepartedToGuardianAndChildren() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT, calledChildren()));

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		assertPublishedOnce("VEHICLE_DEPARTED", DEPARTED_AT, GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("완료(COMPLETED)된 세션에도 DEPARTED를 받아 sessionStatus는 그대로 두고 vehiclePhase DEPARTED와 VEHICLE_DEPARTED를 저장한다")
	void departedAfterCompletionIsAccepted() {
		givenLockedSession(completedSession());

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		PickupSession saved = savedSession();
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.DEPARTED);
		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("VEHICLE_DEPARTED"), eq(saved.getStateVersion()), eq(DEPARTED_AT), any());
	}

	// #107 DOOR_CYCLE 탑승 후보

	@ParameterizedTest(name = "호출 {0}")
	@EnumSource(value = CallStatus.class, names = {"SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("정차 중 · 합류 안내 발송 뒤의 DOOR_CYCLE은 boardingDetectedAt · boardingCancelableUntil(+10초) · boardingDoor와 모든 자녀 boardedAt을 채워 저장한다")
	void doorCycleAfterCallSentRecordsBoarding(CallStatus callStatus) {
		givenLockedSession(stoppedSession());
		givenLiveCall(call(callStatus));

		receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT, "REAR_RIGHT"));

		PickupSession saved = savedSession();
		assertThat(saved.getBoardingDetectedAt()).isEqualTo(DOOR_AT);
		assertThat(saved.getBoardingCancelableUntil()).isEqualTo(DOOR_AT.plus(CANCEL_WINDOW));
		assertThat(saved.getBoardingDoor()).isEqualTo(BoardingDoor.REAR_RIGHT);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(saved.getChildren()).hasSize(2);
		assertThat(saved.getChildren())
				.allSatisfy(child -> assertThat(child.getBoardedAt()).as("boardedAt").isEqualTo(DOOR_AT));
	}

	@Test
	@DisplayName("탑승 후보가 된 DOOR_CYCLE은 BOARDING_DETECTED를 저장한 세션의 stateVersion과 문 열림 시각 occurredAt으로 한 번 저장한다")
	void doorCycleCandidateAppendsBoardingDetected() {
		givenLockedSession(stoppedSession());
		givenLiveCall(call(CallStatus.SENT));

		receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT, "REAR_RIGHT"));

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("BOARDING_DETECTED"), eq(STATE_VERSION + 1), eq(DOOR_AT), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("BOARDING_DETECTED");
	}

	@Test
	@DisplayName("탑승 후보가 된 DOOR_CYCLE은 저장한 BOARDING_DETECTED를 append가 준 eventId · seq · payload로 보호자에게만 발행한다")
	void doorCycleCandidatePublishesBoardingDetectedToGuardianOnly() {
		givenLockedSession(stoppedSession());
		givenLiveCall(call(CallStatus.SENT));

		receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT, "REAR_RIGHT"));

		assertPublishedOnce("BOARDING_DETECTED", DOOR_AT, GUARDIAN_ID);
	}

	@Test
	@DisplayName("정차 중이 아니면(APPROACHING) DOOR_CYCLE은 탑승 후보가 아니어서 탑승을 기록하지 않고 BOARDING_DETECTED를 저장 · 발행하지 않는다")
	void doorCycleWhileNotStoppedIsIgnored() {
		PickupSession approaching = approachingSession(PREDICTED_STOP_AT);
		givenLockedSession(approaching);
		givenLiveCall(call(CallStatus.SENT));

		receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT, "REAR_RIGHT"));

		assertNoBoardingRecorded(approaching);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("callsBeforeSend")
	@DisplayName("합류 안내 발송 전(호출 없음 · ACCEPTED · 철회됨)이면 정차 중 DOOR_CYCLE도 탑승을 기록하지 않고 BOARDING_DETECTED를 저장 · 발행하지 않는다")
	void doorCycleBeforeCallSentIsIgnored(CallSetup callSetup) {
		PickupSession stopped = stoppedSession();
		givenLockedSession(stopped);
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(callSetup.liveCall());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(callSetup.latestCall());

		receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT, "REAR_RIGHT"));

		assertNoBoardingRecorded(stopped);
	}

	// loadLiveCall은 철회되지 않은 호출만, loadLatestCall은 REVOKED 포함 가장 최근 호출
	static Stream<Named<CallSetup>> callsBeforeSend() {
		Call revoked = call(CallStatus.REVOKED);
		return Stream.of(
				Named.of("호출 없음", new CallSetup(Optional.empty(), Optional.empty())),
				Named.of("ACCEPTED(발송 전)",
						new CallSetup(Optional.of(call(CallStatus.ACCEPTED)), Optional.of(call(CallStatus.ACCEPTED)))),
				Named.of("철회된 호출만 있음", new CallSetup(Optional.empty(), Optional.of(revoked))));
	}

	record CallSetup(Optional<Call> liveCall, Optional<Call> latestCall) {
	}

	private void assertNoBoardingRecorded(PickupSession session) {
		verify(sessionEventPort, never()).append(anyLong(), eq("BOARDING_DETECTED"), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
		assertThat(session.getBoardingDetectedAt()).isNull();
		assertThat(session.getBoardingCancelableUntil()).isNull();
		assertThat(session.getBoardingDoor()).isNull();
		assertThat(session.getStateVersion()).isEqualTo(STATE_VERSION);
		assertThat(session.getChildren())
				.allSatisfy(child -> assertThat(child.getBoardedAt()).as("boardedAt").isNull());
	}

	private void assertPublishedOnce(String type, Instant occurredAt, Long... receiverUserIds) {
		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages).hasSize(1);
		assertThat(appendedEvents).hasSize(1);
		SessionEventMessage message = messages.get(0);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(message.type()).isEqualTo(type);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(appended.getStateVersion());
		assertThat(message.occurredAt()).isEqualTo(occurredAt);
		assertThat(message.payload()).isEqualTo(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(receiverUserIds);
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

	private static VehicleEventCommand command(String type, Instant occurredAt, String door) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new VehicleEventCommand(vehicle, SESSION_ID, EVENT_ID, type, occurredAt, VEHICLE_LOCATION, door);
	}

	// 합류 안내를 보내 두 자녀가 CALLED이고 차량이 장소로 다가오는 세션
	private static PickupSession approachingSession(Instant etaAt) {
		return session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, etaAt, calledChildren());
	}

	// 차량이 정차해 문 열림을 기다리는 세션
	private static PickupSession stoppedSession() {
		return session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT, calledChildren());
	}

	private static PickupSession session(SessionStatus status, VehiclePhase phase, Instant etaAt,
			List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, phase, SpotStatus.CONFIRMED, STATE_VERSION,
				0, 1, 0, false, etaAt, etaAt == null ? null : 40, etaAt == null ? null : ETA_UPDATED_AT, null, null,
				null, null, null, null, null, children);
	}

	// 문 열림으로 두 자녀가 탔고 취소 창이 지나 자동 완료된 세션(차량은 아직 정차 중)
	private static PickupSession completedSession() {
		List<SessionChild> boarded = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, DOOR_AT),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, DOOR_AT));
		PickupSession session = PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.STOPPED,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, PREDICTED_STOP_AT, 40, ETA_UPDATED_AT, DOOR_AT,
				DOOR_AT.plus(CANCEL_WINDOW), BoardingDoor.REAR_RIGHT, null, null, null, null, boarded);
		session.complete(BoardingMethod.DOOR, List.of(), DOOR_AT.plus(CANCEL_WINDOW).plusSeconds(1));
		return session;
	}

	private static List<SessionChild> calledChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null));
	}

	private static Call call(CallStatus status) {
		Instant acceptedAt = NOW;
		Instant sentAt = status == CallStatus.ACCEPTED ? null : acceptedAt.plusSeconds(1);
		Instant deliveredAt = status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED
				? acceptedAt.plusSeconds(2) : null;
		Instant acknowledgedAt = status == CallStatus.ACKNOWLEDGED ? acceptedAt.plusSeconds(30) : null;
		Instant revokedAt = status == CallStatus.REVOKED ? acceptedAt.plusSeconds(40) : null;
		String revokeReason = status == CallStatus.REVOKED ? "장소를 다시 정합니다" : null;
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, 0, acceptedAt, sentAt,
				deliveredAt, acknowledgedAt, revokedAt, revokeReason);
	}

}

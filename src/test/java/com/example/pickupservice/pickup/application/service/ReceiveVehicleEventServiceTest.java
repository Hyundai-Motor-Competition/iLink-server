package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.VehicleEventCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.ProcessedVehicleEventPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
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
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long DELEGATE_ID = 35L;
	private static final long TEACHER_ID = 36L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long LIVE_CALL_ID = 3001L;
	private static final long NEW_CALL_ID = 3002L;
	private static final LatLng CHILD_LOCATION = new LatLng(37.501350, 127.039900);
	private static final String EVENT_ID = "6f1c2b9e-3d4a-4e8b-9a71-0c5d2e8f4b13";

	private static final int STATE_VERSION = 8;
	private static final long LAST_SEQ = 7L;
	private static final long EVENT_ID_BASE = 9000L;
	/** approachingSession()의 etaAt(08:51:30) - etaUpdatedAt(08:51:20) = 저장된 ETA 기준 남은 초 */
	private static final int STORED_NAV_ETA_SECONDS = 10;
	private static final int STORED_REMAINING_METERS = 40;

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

	@Mock
	private SaveCallPort saveCallPort;

	@Mock
	private RecordNotificationPort recordNotificationPort;

	@Mock
	private CallEngine mockCallEngine;

	private ReceiveVehicleEventService receiveVehicleEventService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		receiveVehicleEventService = new ReceiveVehicleEventService(loadSessionPort, saveSessionPort, loadCallPort,
				loadFamilyPort, processedVehicleEventPort, sessionEventPort, publishSessionEventPort, PROPERTIES,
				saveCallPort, mockCallEngine);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(saveCallPort.saveCall(any(Call.class))).thenAnswer(invocation -> {
			Call call = invocation.getArgument(0);
			return Call.restore(call.getCallId() == null ? NEW_CALL_ID : call.getCallId(), call.getSessionId(),
					call.getPrevCallId(), call.getTriggerType(), call.getStatus(), call.getSpotVersion(),
					call.getAcceptedAt(), call.getSentAt(), call.getDeliveredAt(), call.getAcknowledgedAt(),
					call.getRevokedAt(), call.getRevokeReason());
		});
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
		assertThat(appendedEvents.get(0).getPayload())
				.containsEntry("predictedStopAt", TimeUtils.toKst(PREDICTED_STOP_AT));
	}

	@Test
	@DisplayName("VEHICLE_STOPPED payload의 predictedStopAt은 저장 · 발행 모두 TimeUtils.toKst와 같은 +09:00 문자열이다")
	void stoppedPayloadPredictedStopAtIsKstOffsetString() {
		givenLockedSession(approachingSession(PREDICTED_STOP_AT));

		receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null));

		Object appended = appendedEvents.get(0).getPayload().get("predictedStopAt");
		assertThat(appended).isEqualTo(TimeUtils.toKst(PREDICTED_STOP_AT)).isEqualTo("2026-11-09T17:51:30+09:00");
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		SessionEventMessage published = captor.getAllValues().stream()
				.filter(message -> "VEHICLE_STOPPED".equals(message.type()))
				.findFirst()
				.orElseThrow();
		assertThat(published.payload().get("predictedStopAt")).isEqualTo("2026-11-09T17:51:30+09:00");
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

	// #245 DEPARTED 자동 철회

	@ParameterizedTest(name = "호출 {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("차량 이벤트 DEPARTED는 철회되지 않은 호출을 REVOKED · revokedAt · revokeReason VEHICLE_DEPARTED로 바꿔 저장한다")
	void departedRevokesLiveCallWithVehicleDepartedReason(CallStatus liveStatus) {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT, locatedCalledChildren()));
		Call live = call(liveStatus);
		givenLiveCall(live);

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		Call saved = captor.getValue();
		assertThat(saved.getCallId()).isEqualTo(LIVE_CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(saved.getRevokedAt()).isNotNull();
		assertThat(saved.getRevokeReason()).isEqualTo("VEHICLE_DEPARTED");
	}

	@Test
	@DisplayName("차량 이벤트 DEPARTED로 호출을 철회하면 세션 자녀를 모두 WAITING으로 돌리고 자녀 위치를 비우며 vehiclePhase는 DEPARTED로 저장한다")
	void departedRevertsChildrenToWaitingAndClearsLocation() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT, locatedCalledChildren()));
		givenLiveCall(call(CallStatus.SENT));

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.DEPARTED);
		assertThat(saved.getStateVersion()).isGreaterThan(STATE_VERSION);
		assertThat(saved.getChildren()).hasSize(2).allSatisfy(child -> {
			assertThat(child.getChildPhase()).isEqualTo(ChildPhase.WAITING);
			assertThat(child.getLastLocation()).isNull();
			assertThat(child.getLastLocatedAt()).isNull();
		});
	}

	@Test
	@DisplayName("차량 이벤트 DEPARTED로 호출을 철회하면 VEHICLE_DEPARTED와 CALL_REVOKED를 모두 저장하고 둘 다 보호자와 세션 자녀에게 발행한다")
	void departedAppendsAndPublishesVehicleDepartedAndCallRevoked() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT, locatedCalledChildren()));
		givenLiveCall(call(CallStatus.SENT));

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		PickupSession saved = savedSession();
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactlyInAnyOrder("VEHICLE_DEPARTED", "CALL_REVOKED");
		verify(sessionEventPort).append(eq(SESSION_ID), eq("CALL_REVOKED"), eq(saved.getStateVersion()), any(Instant.class),
				any());
		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages)
				.extracting(SessionEventMessage::type)
				.containsExactlyInAnyOrder("VEHICLE_DEPARTED", "CALL_REVOKED");
		assertThat(messages).allSatisfy(message -> assertThat(message.receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID));
		for (SessionEvent appended : appendedEvents) {
			SessionEventMessage message = messages.stream()
					.filter(candidate -> candidate.type().equals(appended.getType().name()))
					.findFirst()
					.orElseThrow();
			assertThat(message.eventId()).isEqualTo(appended.getEventId());
			assertThat(message.seq()).isEqualTo(appended.getSeq());
		}
	}

	@Test
	@DisplayName("차량 이벤트 DEPARTED는 철회되지 않은 호출이 없으면 호출 저장과 CALL_REVOKED 없이 VEHICLE_DEPARTED만 저장 · 발행하고 자녀는 그대로 둔다")
	void departedWithoutLiveCallAppendsOnlyVehicleDeparted() {
		PickupSession stopped = session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT,
				locatedCalledChildren());
		givenLockedSession(stopped);
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call(CallStatus.REVOKED)));

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		verify(saveCallPort, never()).saveCall(any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_DEPARTED");
		assertThat(publishedMessages())
				.extracting(SessionEventMessage::type)
				.containsExactly("VEHICLE_DEPARTED");
		assertThat(stopped.getChildren()).allSatisfy(child -> {
			assertThat(child.getChildPhase()).isEqualTo(ChildPhase.CALLED);
			assertThat(child.getLastLocation()).isEqualTo(CHILD_LOCATION);
		});
	}

	// #246 STOP_FAILED

	@ParameterizedTest(name = "호출 {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("차량 이벤트 STOP_FAILED는 철회되지 않은 호출을 REVOKED · revokedAt · revokeReason STOP_FAILED로 바꿔 저장한다")
	void stopFailedRevokesLiveCallWithStopFailedReason(CallStatus liveStatus) {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, PREDICTED_STOP_AT,
				locatedCalledChildren()));
		givenLiveCall(call(liveStatus));

		receiveVehicleEventService.receive(command("STOP_FAILED", STOPPED_AT, null));

		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		Call saved = captor.getValue();
		assertThat(saved.getCallId()).isEqualTo(LIVE_CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(saved.getRevokedAt()).isNotNull();
		assertThat(saved.getRevokeReason()).isEqualTo("STOP_FAILED");
	}

	@Test
	@DisplayName("차량 이벤트 STOP_FAILED로 호출을 철회하면 vehiclePhase는 그대로 두고 세션 자녀를 WAITING으로 돌리고 위치를 비운다")
	void stopFailedKeepsVehiclePhaseAndRevertsChildrenToWaiting() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, PREDICTED_STOP_AT,
				locatedCalledChildren()));
		givenLiveCall(call(CallStatus.SENT));

		receiveVehicleEventService.receive(command("STOP_FAILED", STOPPED_AT, null));

		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.APPROACHING);
		assertThat(saved.getChildren()).hasSize(2).allSatisfy(child -> {
			assertThat(child.getChildPhase()).isEqualTo(ChildPhase.WAITING);
			assertThat(child.getLastLocation()).isNull();
			assertThat(child.getLastLocatedAt()).isNull();
		});
	}

	@Test
	@DisplayName("차량 이벤트 STOP_FAILED로 호출을 철회하면 CALL_REVOKED만 저장하고 보호자와 세션 자녀에게 발행한다")
	void stopFailedAppendsAndPublishesCallRevokedOnly() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, PREDICTED_STOP_AT,
				locatedCalledChildren()));
		givenLiveCall(call(CallStatus.SENT));

		receiveVehicleEventService.receive(command("STOP_FAILED", STOPPED_AT, null));

		PickupSession saved = savedSession();
		assertThat(saved.getStateVersion()).isGreaterThan(STATE_VERSION);
		verify(sessionEventPort).append(eq(SESSION_ID), eq("CALL_REVOKED"), eq(saved.getStateVersion()), any(Instant.class),
				any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("CALL_REVOKED");
		assertPublishedOnce("CALL_REVOKED", appendedEvents.get(0).getOccurredAt(), GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("차량 이벤트 STOP_FAILED는 철회되지 않은 호출이 없으면 세션 · 호출 저장, 이벤트 저장 · 발행을 하지 않고 eventId만 처리한 것으로 남긴다")
	void stopFailedWithoutLiveCallChangesNothing() {
		PickupSession approaching = session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, PREDICTED_STOP_AT,
				locatedCalledChildren());
		givenLockedSession(approaching);
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call(CallStatus.REVOKED)));

		receiveVehicleEventService.receive(command("STOP_FAILED", STOPPED_AT, null));

		verify(processedVehicleEventPort).markProcessed(SESSION_ID, EVENT_ID);
		verify(saveSessionPort, never()).save(any());
		verify(saveCallPort, never()).saveCall(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
		assertThat(approaching.getVehiclePhase()).isEqualTo(VehiclePhase.APPROACHING);
		assertThat(approaching.getStateVersion()).isEqualTo(STATE_VERSION);
		assertThat(approaching.getChildren()).allSatisfy(child -> {
			assertThat(child.getChildPhase()).isEqualTo(ChildPhase.CALLED);
			assertThat(child.getLastLocation()).isEqualTo(CHILD_LOCATION);
		});
	}

	// #264 끝난 세션 · 탑승 인식 뒤에는 자동 철회하지 않는다

	@ParameterizedTest(name = "{0}")
	@MethodSource("finishedOrBoardedSessions")
	@DisplayName("끝난(COMPLETED) 세션이거나 탑승을 인식한 세션의 DEPARTED는 살아 있는 호출을 철회하지 않고 자녀 상태 · acknowledgedAt을 바꾸지 않으며 vehiclePhase DEPARTED와 VEHICLE_DEPARTED만 남긴다")
	void departedAfterCompletionOrBoardingDoesNotRevokeLiveCall(Supplier<PickupSession> sessionSupplier) {
		PickupSession session = sessionSupplier.get();
		givenLockedSession(session);
		givenLiveCall(call(CallStatus.ACKNOWLEDGED));
		List<ChildSnapshot> childrenBefore = snapshotOf(session);

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		verify(saveCallPort, never()).saveCall(any());
		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.DEPARTED);
		assertThat(snapshotOf(saved)).isEqualTo(childrenBefore);
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_DEPARTED");
		assertThat(publishedMessages())
				.extracting(SessionEventMessage::type)
				.containsExactly("VEHICLE_DEPARTED");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("finishedOrBoardedSessions")
	@DisplayName("끝난(COMPLETED) 세션이거나 탑승을 인식한 세션의 STOP_FAILED는 살아 있는 호출을 철회하지 않고 자녀 상태 · acknowledgedAt을 바꾸지 않으며 CALL_REVOKED를 저장 · 발행하지 않는다")
	void stopFailedAfterCompletionOrBoardingDoesNotRevokeLiveCall(Supplier<PickupSession> sessionSupplier) {
		PickupSession session = sessionSupplier.get();
		givenLockedSession(session);
		givenLiveCall(call(CallStatus.ACKNOWLEDGED));
		List<ChildSnapshot> childrenBefore = snapshotOf(session);

		receiveVehicleEventService.receive(command("STOP_FAILED", STOPPED_AT, null));

		verify(saveCallPort, never()).saveCall(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
		assertThat(snapshotOf(session)).isEqualTo(childrenBefore);
	}

	static Stream<Named<Supplier<PickupSession>>> finishedOrBoardedSessions() {
		return Stream.of(
				Named.of("COMPLETED 세션(NOT_BOARDED 자녀 포함)",
						ReceiveVehicleEventServiceTest::completedSessionWithNotBoardedChild),
				Named.of("ACTIVE이지만 boardingDetectedAt 있는 세션",
						ReceiveVehicleEventServiceTest::boardingDetectedActiveSession));
	}

	private record ChildSnapshot(long childUserId, ChildPhase phase, Instant acknowledgedAt, LatLng lastLocation) {
	}

	private static List<ChildSnapshot> snapshotOf(PickupSession session) {
		return session.getChildren().stream()
				.map(child -> new ChildSnapshot(child.getChildUserId(), child.getChildPhase(), child.getAcknowledgedAt(),
						child.getLastLocation()))
				.toList();
	}

	// #266 탑승 인식 없이 MANUAL로 끝난 세션 · CANCELLED 세션은 boardingDetectedAt이 없어도 자동 철회하지 않는다

	@ParameterizedTest(name = "{0} · 호출 {1}")
	@MethodSource("manualCompletedOrCancelledSessionsWithLiveCall")
	@DisplayName("탑승 인식 없이 MANUAL로 끝난(COMPLETED) 세션이나 CANCELLED 세션의 DEPARTED는 살아 있는 호출을 철회하지 않고 자녀 상태 · acknowledgedAt을 바꾸지 않으며 vehiclePhase DEPARTED와 VEHICLE_DEPARTED만 남긴다")
	void departedAfterManualCompletionOrCancellationDoesNotRevokeLiveCall(Supplier<PickupSession> sessionSupplier,
			CallStatus liveStatus) {
		PickupSession session = sessionSupplier.get();
		assertThat(session.getBoardingDetectedAt()).isNull();
		givenLockedSession(session);
		givenLiveCall(call(liveStatus));
		List<ChildSnapshot> childrenBefore = snapshotOf(session);

		receiveVehicleEventService.receive(command("DEPARTED", DEPARTED_AT, null));

		verify(saveCallPort, never()).saveCall(any());
		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.DEPARTED);
		assertThat(snapshotOf(saved)).isEqualTo(childrenBefore);
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_DEPARTED");
		assertThat(publishedMessages())
				.extracting(SessionEventMessage::type)
				.containsExactly("VEHICLE_DEPARTED");
	}

	@ParameterizedTest(name = "{0} · 호출 {1}")
	@MethodSource("manualCompletedOrCancelledSessionsWithLiveCall")
	@DisplayName("탑승 인식 없이 MANUAL로 끝난(COMPLETED) 세션이나 CANCELLED 세션의 STOP_FAILED는 살아 있는 호출을 철회하지 않고 자녀 상태 · acknowledgedAt을 바꾸지 않으며 CALL_REVOKED를 저장 · 발행하지 않는다")
	void stopFailedAfterManualCompletionOrCancellationDoesNotRevokeLiveCall(Supplier<PickupSession> sessionSupplier,
			CallStatus liveStatus) {
		PickupSession session = sessionSupplier.get();
		assertThat(session.getBoardingDetectedAt()).isNull();
		givenLockedSession(session);
		givenLiveCall(call(liveStatus));
		List<ChildSnapshot> childrenBefore = snapshotOf(session);

		receiveVehicleEventService.receive(command("STOP_FAILED", STOPPED_AT, null));

		verify(saveCallPort, never()).saveCall(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
		assertThat(snapshotOf(session)).isEqualTo(childrenBefore);
	}

	static Stream<Arguments> manualCompletedOrCancelledSessionsWithLiveCall() {
		return Stream.of(CallStatus.SENT, CallStatus.ACKNOWLEDGED).flatMap(liveStatus -> Stream.of(
				Arguments.of(Named.of("MANUAL로 완료된 세션(boardingDetectedAt 없음)",
						(Supplier<PickupSession>) ReceiveVehicleEventServiceTest::manualCompletedSession), liveStatus),
				Arguments.of(Named.of("CANCELLED 세션(boardingDetectedAt 없음)",
						(Supplier<PickupSession>) ReceiveVehicleEventServiceTest::cancelledSession), liveStatus)));
	}

	// 정차 중 기사가 MANUAL로 완료한 세션. 탑승 인식(문 열림)을 거치지 않아 boardingDetectedAt은 비어 있다
	private static PickupSession manualCompletedSession() {
		PickupSession session = session(SessionStatus.ACTIVE, VehiclePhase.STOPPED, PREDICTED_STOP_AT,
				calledChildren());
		session.complete(BoardingMethod.MANUAL, List.of(), DOOR_AT);
		return session;
	}

	// 차량이 정차한 상태에서 취소된 세션. 탑승 인식 없이 boardingDetectedAt은 비어 있다
	private static PickupSession cancelledSession() {
		return session(SessionStatus.CANCELLED, VehiclePhase.STOPPED, PREDICTED_STOP_AT, calledChildren());
	}

	// #247 STOPPED 뒤 합류 호출 판정 다시 돌리기

	@Test
	@DisplayName("차량 이벤트 STOPPED는 VEHICLE_STOPPED를 저장한 뒤 저장한 세션과 저장된 ETA(etaAt - etaUpdatedAt 초 · remainingMeters)로 호출 판정 triggerIfReady를 한 번 다시 돌린다")
	void stoppedRunsCallDecisionAgainWithStoredEtaAfterVehicleStopped() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, PREDICTED_STOP_AT, waitingChildren()));

		receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null));

		PickupSession saved = savedSession();
		var order = inOrder(sessionEventPort, mockCallEngine);
		order.verify(sessionEventPort).append(eq(SESSION_ID), eq("VEHICLE_STOPPED"), anyInt(), any(Instant.class), any());
		order.verify(mockCallEngine).triggerIfReady(same(saved), eq(STORED_NAV_ETA_SECONDS), eq(STORED_REMAINING_METERS));
	}

	@Test
	@DisplayName("차량 이벤트 STOPPED는 저장된 ETA가 없는 세션이면 오류 없이 VEHICLE_STOPPED만 저장하고 호출 판정을 돌리지 않는다")
	void stoppedWithoutStoredEtaDoesNotRunCallDecision() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.ROUTING, null, waitingChildren()));

		assertThatCode(() -> receiveVehicleEventService.receive(command("STOPPED", STOPPED_AT, null)))
				.doesNotThrowAnyException();

		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_STOPPED");
		verifyNoInteractions(mockCallEngine);
	}

	@Test
	@DisplayName("차량 이벤트 STOPPED는 저장된 ETA가 180초 안이고 가드를 통과하는 AUTO 가족 세션이면 VEHICLE_STOPPED 저장 뒤 ACCEPTED · ETA_3MIN Call을 만든다")
	void stoppedCreatesCallWhenStoredEtaMeetsTriggerAndGuards() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, PREDICTED_STOP_AT, waitingChildren()));

		realCallEngineService().receive(command("STOPPED", STOPPED_AT, null));

		ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
		verify(saveCallPort).saveCall(captor.capture());
		Call created = captor.getValue();
		assertThat(created.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(created.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(created.getTriggerType()).isEqualTo(CallTriggerType.ETA_3MIN);
		assertThat(created.getPrevCallId()).isNull();
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_STOPPED", "CALL_TRIGGERED");
		SessionEvent callTriggered = appendedEvents.get(1);
		assertThat(callTriggered.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(callTriggered.getPayload())
				.containsEntry("callId", NEW_CALL_ID)
				.containsEntry("trigger", "ETA_3MIN");
		verify(recordNotificationPort).recordNotifications(any());
		assertThat(publishedMessages())
				.extracting(SessionEventMessage::type)
				.containsExactly("VEHICLE_STOPPED", "CALL_TRIGGERED");
	}

	@Test
	@DisplayName("차량 이벤트 STOPPED는 호출 보류(callHeld true) 세션이면 VEHICLE_STOPPED만 저장하고 Call · 알림 기록 요청 · CALL_TRIGGERED를 만들지 않는다")
	void stoppedWithCallHeldDoesNotCreateCall() {
		givenLockedSession(session(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, PREDICTED_STOP_AT, waitingChildren(),
				true));

		realCallEngineService().receive(command("STOPPED", STOPPED_AT, null));

		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("VEHICLE_STOPPED");
		verify(saveCallPort, never()).saveCall(any());
		verify(recordNotificationPort, never()).recordNotifications(any());
		assertThat(publishedMessages())
				.extracting(SessionEventMessage::type)
				.containsExactly("VEHICLE_STOPPED");
	}

	// 호출 판정을 실제 CallEngine으로 돌리는 서비스(시계는 정차 시각에 고정해 저장된 ETA가 유효하다)
	private ReceiveVehicleEventService realCallEngineService() {
		Clock clock = Clock.fixed(STOPPED_AT, ZoneOffset.UTC);
		CallEngine callEngine = new CallEngine(loadCallPort, saveCallPort, loadFamilyPort, sessionEventPort,
				publishSessionEventPort, recordNotificationPort, PROPERTIES, clock);
		return new ReceiveVehicleEventService(loadSessionPort, saveSessionPort, loadCallPort, loadFamilyPort,
				processedVehicleEventPort, sessionEventPort, publishSessionEventPort, PROPERTIES, saveCallPort,
				callEngine);
	}

	// #107 DOOR_CYCLE 탑승 후보

	@Test
	@DisplayName("완료(COMPLETED)된 세션에 온 DOOR_CYCLE은 오류 없이 무시하고 eventId는 처리한 것으로 남기며 세션 저장 · 이벤트 저장 · 발행을 하지 않는다")
	void doorCycleOnCompletedSessionIsIgnoredButEventIdIsMarkedProcessed() {
		PickupSession completed = completedSession();
		givenLockedSession(completed);
		givenLiveCall(call(CallStatus.ACKNOWLEDGED));
		int stateVersionBefore = completed.getStateVersion();

		assertThatCode(() -> receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT.plusSeconds(60), "REAR_RIGHT")))
				.doesNotThrowAnyException();

		verify(processedVehicleEventPort).markProcessed(SESSION_ID, EVENT_ID);
		verify(saveSessionPort, never()).save(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
		assertThat(completed.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(completed.getStateVersion()).isEqualTo(stateVersionBefore);
	}

	@Test
	@DisplayName("취소(CANCELLED)된 세션에 온 DOOR_CYCLE은 오류 없이 무시하고 eventId는 처리한 것으로 남기며 세션 저장 · 이벤트 저장 · 발행을 하지 않는다")
	void doorCycleOnCancelledSessionIsIgnoredButEventIdIsMarkedProcessed() {
		PickupSession cancelled = session(SessionStatus.CANCELLED, VehiclePhase.STOPPED, PREDICTED_STOP_AT,
				calledChildren());
		givenLockedSession(cancelled);
		givenLiveCall(call(CallStatus.ACKNOWLEDGED));
		int stateVersionBefore = cancelled.getStateVersion();

		assertThatCode(() -> receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT, "REAR_RIGHT")))
				.doesNotThrowAnyException();

		verify(processedVehicleEventPort).markProcessed(SESSION_ID, EVENT_ID);
		verify(saveSessionPort, never()).save(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
		assertThat(cancelled.getSessionStatus()).isEqualTo(SessionStatus.CANCELLED);
		assertThat(cancelled.getStateVersion()).isEqualTo(stateVersionBefore);
	}

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
	@DisplayName("BOARDING_DETECTED 발행의 받는 사람은 가족의 GUARDIAN · CO_GUARDIAN · DELEGATE 모두이고 TEACHER · CHILD는 빠진다")
	void doorCycleCandidatePublishesBoardingDetectedToGuardianCoGuardianAndDelegateWithoutTeacher() {
		givenLockedSession(stoppedSession());
		givenLiveCall(call(CallStatus.SENT));
		when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444"),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666"),
				new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "최선생", "010-7777-8888"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));

		receiveVehicleEventService.receive(command("DOOR_CYCLE", DOOR_AT, "REAR_RIGHT"));

		assertPublishedOnce("BOARDING_DETECTED", DOOR_AT, GUARDIAN_ID, CO_GUARDIAN_ID, DELEGATE_ID);
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
		return session(status, phase, etaAt, children, false);
	}

	private static PickupSession session(SessionStatus status, VehiclePhase phase, Instant etaAt,
			List<SessionChild> children, boolean callHeld) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, phase, SpotStatus.CONFIRMED, STATE_VERSION,
				0, 1, 0, callHeld, etaAt, etaAt == null ? null : 40, etaAt == null ? null : ETA_UPDATED_AT, null, null,
				null, null, null, null, null, null, children);
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
				DOOR_AT.plus(CANCEL_WINDOW), BoardingDoor.REAR_RIGHT, null, null, null, null, null, boarded);
		session.complete(BoardingMethod.DOOR, List.of(), DOOR_AT.plus(CANCEL_WINDOW).plusSeconds(1));
		return session;
	}

	// 민준은 탔고 서윤은 나오지 않아 완료 때 NOT_BOARDED가 된 COMPLETED 세션(둘 다 합류 안내를 확인한 적 있음)
	private static PickupSession completedSessionWithNotBoardedChild() {
		PickupSession session = PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.STOPPED,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, PREDICTED_STOP_AT, 40, ETA_UPDATED_AT, DOOR_AT,
				DOOR_AT.plus(CANCEL_WINDOW), BoardingDoor.REAR_RIGHT, null, null, null, null, null,
				acknowledgedChildren(DOOR_AT, null));
		session.complete(BoardingMethod.DOOR, List.of(SEOYUN_ID), DOOR_AT.plus(CANCEL_WINDOW).plusSeconds(1));
		return session;
	}

	// 문 열림을 인식해(boardingDetectedAt 있음) 아직 취소 창 안이라 ACTIVE인 세션. 두 자녀는 합류 안내를 확인해 MEETING
	private static PickupSession boardingDetectedActiveSession() {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.STOPPED,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, PREDICTED_STOP_AT, 40, ETA_UPDATED_AT, DOOR_AT,
				DOOR_AT.plus(CANCEL_WINDOW), BoardingDoor.REAR_RIGHT, null, null, null, null, null,
				acknowledgedChildren(DOOR_AT, DOOR_AT));
	}

	private static List<SessionChild> acknowledgedChildren(Instant minjunBoardedAt, Instant seoyunBoardedAt) {
		Instant acknowledgedAt = NOW.plusSeconds(30);
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, CHILD_LOCATION,
						NOW, acknowledgedAt, minjunBoardedAt),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, CHILD_LOCATION,
						NOW, acknowledgedAt, seoyunBoardedAt));
	}

	// 합류 안내를 받고 집을 나서 위치를 보내는 두 자녀
	private static List<SessionChild> locatedCalledChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, CHILD_LOCATION,
						NOW, null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, CHILD_LOCATION,
						NOW, null, null));
	}

	// 아직 합류 안내를 받지 않은 두 자녀
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

package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.CallGuardView;
import com.example.pickupservice.pickup.application.port.in.TelemetryCommand;
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
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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

@ExtendWith(MockitoExtension.class)
class ReceiveTelemetryServiceTest {

	// 한국 시간 2026-11-09(월) 18:09
	private static final Instant NOW = Instant.parse("2026-11-09T09:09:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	// 차량이 2초 전에 잰 값이 지금 도착했다. 그 15초 전에 직전 SIG-01이 왔다
	private static final Instant OBSERVED_AT = NOW.minusSeconds(2);
	private static final Instant PREVIOUS_OBSERVED_AT = OBSERVED_AT.minusSeconds(15);
	private static final int PREVIOUS_NAV_ETA_SECONDS = 200;
	private static final int PREVIOUS_REMAINING_METERS = 1700;

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
	private static final LatLng VEHICLE_LOCATION = new LatLng(37.498100, 127.027600);
	// 직전 SIG-01(PREVIOUS_OBSERVED_AT)에 들어 있던 차량 위치
	private static final LatLng PREVIOUS_VEHICLE_LOCATION = new LatLng(37.495000, 127.025000);

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

	private ReceiveTelemetryService receiveTelemetryService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	// 한 테스트에서 SIG-01을 15초 간격으로 두 번 보낼 때 시각을 옮긴다
	private final MovableClock clock = new MovableClock(NOW);

	@BeforeEach
	void setUp() {
		CallEngine callEngine = new CallEngine(loadCallPort, saveCallPort, loadFamilyPort, sessionEventPort,
				publishSessionEventPort, recordNotificationPort, PROPERTIES, clock);
		receiveTelemetryService = new ReceiveTelemetryService(loadSessionPort, saveSessionPort,
				publishSessionEventPort, callEngine, loadFamilyPort, sessionEventPort, recordNotificationPort, clock);
		givenJoinCallMode(JoinCallMode.AUTO);
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
		// loadFirst는 이 테스트에서 append한 이벤트 중 그 종류의 첫 이벤트를 돌려준다(SIG-01을 두 번 보내는 테스트용)
		lenient().when(sessionEventPort.loadFirst(anyLong(), anyString())).thenAnswer(invocation -> appendedEvents
				.stream()
				.filter(event -> event.getSessionId() == invocation.<Long>getArgument(0)
						&& event.getType().name().equals(invocation.<String>getArgument(1)))
				.findFirst());
	}

	@Test
	@DisplayName("차량 ETA 수신은 ETA_UPDATED를 저장하지 않고 eventId · seq 없이 지금 stateVersion · observedAt으로 차량에게만(FCM 받는 사람 없음) 발행한다")
	void receiveTelemetryPublishesEtaUpdatedWithoutStoringIt() {
		givenLockedSession(readyForCall().build());

		receiveTelemetryService.receive(farFromSpot());

		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		SessionEventMessage message = publishedMessageOf("ETA_UPDATED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isNull();
		assertThat(message.seq()).isNull();
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION);
		assertThat(message.occurredAt()).isEqualTo(OBSERVED_AT);
		assertThat(message.receiverUserIds()).isEmpty();
	}

	@Test
	@DisplayName("ETA_UPDATED payload의 etaAt은 TimeUtils.toKst와 같은 +09:00 문자열이다")
	void etaUpdatedPayloadEtaAtIsKstOffsetString() {
		givenLockedSession(readyForCall().build());

		receiveTelemetryService.receive(farFromSpot());

		SessionEventMessage message = publishedMessageOf("ETA_UPDATED");
		assertThat(message.payload().get("etaAt"))
				.isEqualTo(TimeUtils.toKst(OBSERVED_AT.plusSeconds(600)))
				.isEqualTo("2026-11-09T18:18:58+09:00");
	}

	@Test
	@DisplayName("차량 ETA 수신은 세션의 etaAt(observedAt + navEtaSeconds) · remainingMeters · etaUpdatedAt만 덮어써 저장하고 stateVersion은 올리지 않는다")
	void receiveTelemetrySavesLatestEtaWithoutRaisingStateVersion() {
		givenLockedSession(readyForCall().build());

		receiveTelemetryService.receive(farFromSpot());

		PickupSession saved = savedSession();
		assertThat(saved.getEtaAt()).isEqualTo(OBSERVED_AT.plusSeconds(600));
		assertThat(saved.getRemainingMeters()).isEqualTo(4200);
		assertThat(saved.getEtaUpdatedAt()).isEqualTo(OBSERVED_AT);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION);
	}

	@Test
	@DisplayName("AUTO 가족에서 ETA 180초 안 트리거와 가드 G1~G5를 통과하면 Call을 ACCEPTED · ETA_3MIN · 지금 spotVersion · prevCallId 없음으로 한 번 저장한다")
	void autoFamilySavesAcceptedCallWhenEtaTriggerAndGuardsPass() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());

		receiveTelemetryService.receive(etaWithin3Minutes());

		Call saved = savedCall();
		assertThat(saved.getCallId()).isNull();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.ETA_3MIN);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION);
		assertThat(saved.getPrevCallId()).isNull();
	}

	@Test
	@DisplayName("AUTO 가족에서 거리 700m 안 트리거와 가드를 통과하면 triggerType DIST_700M인 ACCEPTED Call을 저장한다")
	void autoFamilySavesDistanceTriggeredCallWhenWithin700Meters() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());

		receiveTelemetryService.receive(within700Meters());

		Call saved = savedCall();
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.DIST_700M);
	}

	@Test
	@DisplayName("AUTO 가족에서 호출을 만들면 Call을 저장한 뒤 그 callId로 세션 자녀 모두에게 CALL 알림 기록을 한 번 요청한다")
	void autoFamilyRequestsCallNotificationForSessionChildrenAfterSavingCall() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(etaWithin3Minutes());

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
	@DisplayName("AUTO 가족에서 호출을 만들면 Call을 저장한 뒤 CALL_TRIGGERED를 지금 stateVersion · 서버 시각과 payload callId · trigger로 한 번 저장한다")
	void autoFamilyAppendsCallTriggeredAfterSavingCall() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());

		receiveTelemetryService.receive(etaWithin3Minutes());

		InOrder inOrder = inOrder(saveCallPort, sessionEventPort);
		inOrder.verify(saveCallPort).saveCall(any(Call.class));
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("CALL_TRIGGERED"), eq(STATE_VERSION), eq(NOW), any());
		SessionEvent appended = appendedEventOf("CALL_TRIGGERED");
		assertThat(appended.getPayload())
				.containsEntry("callId", NEW_CALL_ID)
				.containsEntry("trigger", "ETA_3MIN");
	}

	@Test
	@DisplayName("AUTO 가족에서 호출을 만들면 저장한 CALL_TRIGGERED를 append가 준 eventId · seq로 보호자에게 발행한다")
	void autoFamilyPublishesCallTriggeredToGuardianAfterAppending() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());

		receiveTelemetryService.receive(etaWithin3Minutes());

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
		// 발행 payload에는 알림 기록이 돌려준 notificationIds가 더해져서 isEqualTo 대신 containsAllEntriesOf로 바꿨다
		assertThat(message.payload()).containsAllEntriesOf(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("ON_HOLD 세션에서는 ETA · 거리 트리거를 만족해도 Call · CALL 알림 기록 요청 · CALL_TRIGGERED를 만들지 않고 ETA_UPDATED만 발행한다")
	void onHoldSessionDoesNotCreateCallEvenWhenTriggered() {
		givenLockedSession(readyForCall().sessionStatus(SessionStatus.ON_HOLD).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(etaWithin3MinutesAndWithin700Meters());

		assertNoCallCreated();
		assertThat(publishedTypes()).containsExactly("ETA_UPDATED");
	}

	@Test
	@DisplayName("callHeld가 true면 ETA · 거리 트리거를 만족해도 Call · CALL 알림 기록 요청 · CALL_TRIGGERED를 만들지 않고 ETA_UPDATED만 발행한다")
	void heldCallDoesNotCreateCallEvenWhenTriggered() {
		givenLockedSession(readyForCall().callHeld(true).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(etaWithin3MinutesAndWithin700Meters());

		assertNoCallCreated();
		assertThat(publishedTypes()).containsExactly("ETA_UPDATED");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED", "ACKNOWLEDGED"})
	@DisplayName("철회되지 않은 호출이 있으면 트리거와 가드 G1~G5를 통과해도 새 Call · CALL 알림 기록 요청 · CALL_TRIGGERED를 만들지 않고 ETA_UPDATED만 발행한다")
	void liveCallPreventsNewCallEvenWhenTriggerAndGuardsPass(CallStatus liveStatus) {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());
		givenPrepareNoticeAlreadySent();
		givenLiveCall(previousCall(liveStatus));

		receiveTelemetryService.receive(etaWithin3MinutesAndWithin700Meters());

		assertThat(guardViewOf(savedSession())).isEqualTo(new CallGuardView(true, List.of()));
		assertNoCallCreated();
		assertThat(publishedTypes()).containsExactly("ETA_UPDATED");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("guardUnmetCases")
	@DisplayName("트리거를 만족해도 가드를 통과하지 못하면 Call · CALL 알림 기록 요청 · CALL_TRIGGERED를 만들지 않고, 저장된 세션의 세션 뷰 call.guard는 met false · unmet에 그 가드 id를 보인다")
	void unmetGuardPreventsCallAndShowsUnmetGuardInSessionView(PickupSession session, TelemetryCommand command,
			List<String> expectedUnmet) {
		givenLockedSession(session);
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(command);

		assertNoCallCreated();
		assertThat(publishedTypes()).containsExactly("ETA_UPDATED");
		assertThat(guardViewOf(savedSession())).isEqualTo(new CallGuardView(false, expectedUnmet));
	}

	// G1(세션 ACTIVE 아님)은 판정 1단계에서 먼저 걸러져 여기서는 다루지 않는다
	// 다른 가드가 섞이지 않도록 모두 차량 정차 중(G6 통과)으로 준비한다
	static Stream<Arguments> guardUnmetCases() {
		return Stream.of(
				Arguments.of(
						Named.of("장소를 바꾸고 아직 경로 확정 전(G2 · G3)", readyForCall()
								.vehiclePhase(VehiclePhase.STOPPED)
								.spotStatus(SpotStatus.SELECTED)
								.spotVersion(SPOT_VERSION + 1)
								.routeSpotVersion(SPOT_VERSION)
								.build()),
						etaWithin3MinutesAndWithin700Meters(), List.of("G2", "G3")),
				Arguments.of(
						Named.of("확정 경로가 이전 장소 버전의 것(G3)", readyForCall()
								.vehiclePhase(VehiclePhase.STOPPED)
								.spotVersion(SPOT_VERSION + 1)
								.routeSpotVersion(SPOT_VERSION)
								.build()),
						etaWithin3MinutesAndWithin700Meters(), List.of("G3")),
				Arguments.of(
						Named.of("70초 전에 잰 ETA가 늦게 도착해 60초가 지남(G4)", readyForCall()
								.vehiclePhase(VehiclePhase.STOPPED)
								.etaUpdatedAt(NOW.minusSeconds(85))
								.build()),
						telemetry(NOW.minusSeconds(70), 170, 650), List.of("G4")),
				Arguments.of(
						Named.of("민준이 준비 시간을 5분 늘려 callNotBefore가 18:14(G5)", readyForCall()
								.vehiclePhase(VehiclePhase.STOPPED)
								.children(List.of(
										child(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING,
												NOW.plus(Duration.ofMinutes(5))),
										child(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, null)))
								.build()),
						etaWithin3MinutesAndWithin700Meters(), List.of("G5")));
	}

	@ParameterizedTest(name = "vehiclePhase {0}")
	@EnumSource(value = VehiclePhase.class, names = {"ROUTING", "APPROACHING"})
	@DisplayName("정차 전(vehiclePhase STOPPED 아님)에는 AUTO 가족에서 ETA 180초 안 트리거와 가드 G1~G5를 만족해도 Call · CALL 알림 기록 요청 · CALL_TRIGGERED를 만들지 않고, 저장된 세션의 세션 뷰 call.guard는 met false · unmet [G6]이다")
	void vehicleNotStoppedPreventsAutoCallAndShowsG6InSessionView(VehiclePhase notStopped) {
		// ETA 170초 · 남은 거리 1500m: ETA_3MIN만 맞고 ROUTING → APPROACHING 전환 조건(700m · 60초)은 아니다
		givenLockedSession(readyForCall().vehiclePhase(notStopped).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(etaWithin3Minutes());

		assertNoCallCreated();
		assertThat(guardViewOf(savedSession())).isEqualTo(new CallGuardView(false, List.of("G6")));
	}

	@Test
	@DisplayName("GUARDIAN_CONFIRM 가족에서는 트리거와 가드 G1~G5를 통과해도 Call · CALL 알림 기록 요청 · CALL_TRIGGERED를 자동으로 만들지 않고 ETA_UPDATED만 발행한다")
	void guardianConfirmFamilyDoesNotCreateCallAutomatically() {
		givenJoinCallMode(JoinCallMode.GUARDIAN_CONFIRM);
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(etaWithin3MinutesAndWithin700Meters());

		assertThat(guardViewOf(savedSession())).isEqualTo(new CallGuardView(true, List.of()));
		assertNoCallCreated();
		assertThat(publishedTypes()).containsExactly("ETA_UPDATED");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("callNotBeforeBeforeNextTelemetry")
	@DisplayName("callNotBefore 전에는 ETA 트리거를 만족해도 호출을 만들지 않고, callNotBefore가 지난 뒤 첫 차량 ETA 수신에서 ACCEPTED Call과 CALL_TRIGGERED를 한 번 만든다")
	void callWaitsUntilCallNotBeforeAndIsCreatedOnFirstTelemetryAfterIt(Duration untilCallNotBefore) {
		givenLockedSession(readyForCall()
				.vehiclePhase(VehiclePhase.STOPPED)
				.children(List.of(
						child(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, NOW.plus(untilCallNotBefore)),
						child(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, null)))
				.build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(etaWithin3Minutes());

		assertNoCallCreated();

		Instant nextNow = NOW.plusSeconds(15);
		clock.moveTo(nextNow);
		receiveTelemetryService.receive(telemetry(OBSERVED_AT.plusSeconds(15), 155, 1300));

		Call saved = savedCall();
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.ETA_3MIN);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION);
		verify(sessionEventPort).append(eq(SESSION_ID), eq("CALL_TRIGGERED"), eq(STATE_VERSION), eq(nextNow), any());
	}

	// 다음 SIG-01은 15초 뒤(18:09:15)에 온다. 그때 callNotBefore가 이미 지났거나, 딱 그 시각이다(같으면 지난 것으로 봄)
	static Stream<Named<Duration>> callNotBeforeBeforeNextTelemetry() {
		return Stream.of(
				Named.of("callNotBefore 18:09:10, 다음 수신 18:09:15", Duration.ofSeconds(10)),
				Named.of("callNotBefore 18:09:15, 다음 수신과 같은 시각", Duration.ofSeconds(15)));
	}

	@Test
	@DisplayName("가장 최근 호출이 철회됐고 조건이 다시 맞으면 prevCallId에 철회된 호출 id를 단 ACCEPTED Call을 지금 spotVersion으로 한 번 저장하고 CALL_TRIGGERED payload callId는 새 호출 id다")
	void revokedLatestCallLeadsToNewCallLinkedByPrevCallId() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.STOPPED).build());
		givenRevokedLatestCall(previousCall(CallStatus.REVOKED));

		receiveTelemetryService.receive(etaWithin3Minutes());

		Call saved = savedCall();
		assertThat(saved.getCallId()).isNull();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getPrevCallId()).isEqualTo(PREV_CALL_ID);
		assertThat(saved.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(saved.getTriggerType()).isEqualTo(CallTriggerType.ETA_3MIN);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION);
		assertThat(appendedEventOf("CALL_TRIGGERED").getPayload()).containsEntry("callId", NEW_CALL_ID);
	}

	@Test
	@DisplayName("observedAt이 세션 etaUpdatedAt보다 오래된 차량 ETA 수신은 무시해 세션을 저장하지 않고 ETA_UPDATED를 발행하지 않으며 트리거와 가드를 만족해도 호출을 만들지 않는다")
	void telemetryObservedBeforeEtaUpdatedAtIsIgnored() {
		PickupSession session = readyForCall().build();
		givenLockedSession(session);

		// 직전 SIG-01(18:08:43에 잼)보다 5초 먼저 잰 값이 늦게 도착했다. ETA 170초 · 650m라 트리거는 맞는다
		receiveTelemetryService.receive(telemetry(PREVIOUS_OBSERVED_AT.minusSeconds(5), 170, 650));

		verify(saveSessionPort, never()).save(any());
		verify(publishSessionEventPort, never()).publish(any());
		assertNoCallCreated();
		assertThat(session.getEtaUpdatedAt()).isEqualTo(PREVIOUS_OBSERVED_AT);
		assertThat(session.getRemainingMeters()).isEqualTo(PREVIOUS_REMAINING_METERS);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("끝난(COMPLETED · CANCELLED) 세션의 차량 ETA 수신은 오류 없이 무시해 ETA를 저장하지 않고 ETA_UPDATED를 발행하지 않으며 트리거를 만족해도 호출을 만들지 않는다")
	void telemetryOnFinishedSessionIsIgnored(SessionStatus finished) {
		PickupSession session = readyForCall().sessionStatus(finished).build();
		givenLockedSession(session);

		// 직전 SIG-01보다 새로 잰 값이고 ETA 170초 · 650m라 트리거는 맞는다
		receiveTelemetryService.receive(etaWithin3MinutesAndWithin700Meters());

		verify(saveSessionPort, never()).save(any());
		verify(publishSessionEventPort, never()).publish(any());
		assertNoCallCreated();
		assertThat(session.getEtaUpdatedAt()).isEqualTo(PREVIOUS_OBSERVED_AT);
		assertThat(session.getEtaAt()).isEqualTo(PREVIOUS_OBSERVED_AT.plusSeconds(PREVIOUS_NAV_ETA_SECONDS));
		assertThat(session.getRemainingMeters()).isEqualTo(PREVIOUS_REMAINING_METERS);
	}

	// ── 접근 중(VEHICLE_APPROACHING, 700m · 60초) ──

	@ParameterizedTest(name = "navEtaSeconds {0} · remainingMeters {1}")
	@CsvSource({"600, 700", "60, 1500"})
	@DisplayName("ROUTING 세션에서 차량 ETA 수신이 remainingMeters ≤ 700 또는 navEtaSeconds ≤ 60이면 vehiclePhase APPROACHING · stateVersion +1로 저장하고 VEHICLE_APPROACHING을 저장된 stateVersion으로 한 번 저장한다")
	void routingSessionBecomesApproachingAndAppendsVehicleApproaching(int navEtaSeconds, int remainingMeters) {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.ROUTING).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(telemetry(OBSERVED_AT, navEtaSeconds, remainingMeters));

		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.APPROACHING);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("VEHICLE_APPROACHING"), eq(STATE_VERSION + 1), any(Instant.class), any());
	}

	@Test
	@DisplayName("차량 ETA 수신으로 APPROACHING이 되면 저장한 VEHICLE_APPROACHING을 append가 준 eventId · seq로 보호자와 세션 자녀에게 발행한다")
	void vehicleApproachingIsPublishedToGuardianAndSessionChildren() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.ROUTING).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(telemetry(OBSERVED_AT, 60, 1500));

		SessionEvent appended = appendedEventOf("VEHICLE_APPROACHING");
		SessionEventMessage message = publishedMessageOf("VEHICLE_APPROACHING");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("ROUTING에서 차량 ETA 수신으로 APPROACHING이 된 뒤 15초 뒤 신호도 접근 중 기준 안이면 VEHICLE_APPROACHING을 다시 저장 · 발행하지 않아 세션에 한 번만 남는다")
	void vehicleApproachingIsAppendedOnlyOncePerSession() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.ROUTING).build());
		givenPrepareNoticeAlreadySent();

		receiveTelemetryService.receive(telemetry(OBSERVED_AT, 60, 800));
		clock.moveTo(NOW.plusSeconds(15));
		receiveTelemetryService.receive(telemetry(OBSERVED_AT.plusSeconds(15), 45, 600));

		verify(sessionEventPort).append(anyLong(), eq("VEHICLE_APPROACHING"), anyInt(), any(), any());
		assertThat(publishedTypes()).filteredOn("VEHICLE_APPROACHING"::equals).hasSize(1);
	}

	// ── 실내 준비 알림(PREPARE · PREPARE_NOTICE_SENT, 700m · 180초) ──

	@ParameterizedTest(name = "navEtaSeconds {0} · remainingMeters {1}")
	@CsvSource({"180, 1500", "600, 700"})
	@DisplayName("차량 ETA 수신에서 remainingMeters ≤ 700 또는 navEtaSeconds ≤ 180이 처음 되면 세션 자녀 모두에게 PREPARE 알림 기록을 callId 없이 한 번 요청한다")
	void firstTelemetryWithinPrepareThresholdRequestsPrepareNotificationForSessionChildren(int navEtaSeconds,
			int remainingMeters) {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.ROUTING).build());

		receiveTelemetryService.receive(telemetry(OBSERVED_AT, navEtaSeconds, remainingMeters));

		ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(requestCaptor.capture());
		NotificationRequest request = requestCaptor.getValue();
		assertThat(request.sessionId()).isEqualTo(SESSION_ID);
		assertThat(request.callId()).isNull();
		assertThat(request.type()).isEqualTo("PREPARE");
		assertThat(request.receiverUserIds()).containsExactlyInAnyOrder(MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("차량 ETA 수신에서 준비 알림 기준(700m · 180초)이 처음 되면 PREPARE_NOTICE_SENT를 세션의 stateVersion으로 한 번 저장하고 append가 준 eventId · seq로 보호자에게 발행한다")
	void firstTelemetryWithinPrepareThresholdAppendsAndPublishesPrepareNoticeSentToGuardian() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.ROUTING).build());

		// ETA 170초 · 1500m: 준비 알림 기준만 맞고 접근 중 기준(700m · 60초)은 아니다
		receiveTelemetryService.receive(etaWithin3Minutes());

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("PREPARE_NOTICE_SENT"), eq(STATE_VERSION), any(Instant.class), any());
		SessionEvent appended = appendedEventOf("PREPARE_NOTICE_SENT");
		SessionEventMessage message = publishedMessageOf("PREPARE_NOTICE_SENT");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION);
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("준비 알림 기준이 처음 된 뒤 15초 뒤 신호도 기준 안이면 PREPARE 알림 기록 요청과 PREPARE_NOTICE_SENT 저장 · 발행을 다시 하지 않아 세션에 한 번만 남는다")
	void prepareNoticeIsSentOnlyOncePerSession() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.ROUTING).build());

		receiveTelemetryService.receive(etaWithin3Minutes());
		clock.moveTo(NOW.plusSeconds(15));
		receiveTelemetryService.receive(telemetry(OBSERVED_AT.plusSeconds(15), 155, 1300));

		verify(recordNotificationPort).recordNotifications(any(NotificationRequest.class));
		verify(sessionEventPort).append(anyLong(), eq("PREPARE_NOTICE_SENT"), anyInt(), any(), any());
		assertThat(publishedTypes()).filteredOn("PREPARE_NOTICE_SENT"::equals).hasSize(1);
	}

	@Test
	@DisplayName("ON_HOLD 세션은 준비 알림 기준을 넘어도 PREPARE 알림 기록 요청 · PREPARE_NOTICE_SENT를 하지 않고, 보류가 풀린 뒤 첫 차량 ETA 수신에서 기준 안이면 한 번 보낸다")
	void onHoldSessionSkipsPrepareNoticeAndSendsAfterHoldReleased() {
		PickupSession session = readyForCall().sessionStatus(SessionStatus.ON_HOLD).vehiclePhase(VehiclePhase.ROUTING)
				.build();
		givenLockedSession(session);

		receiveTelemetryService.receive(etaWithin3Minutes());

		verify(recordNotificationPort, never()).recordNotifications(any());
		verify(sessionEventPort, never()).append(anyLong(), eq("PREPARE_NOTICE_SENT"), anyInt(), any(), any());
		assertThat(publishedTypes()).doesNotContain("PREPARE_NOTICE_SENT");

		session.releaseHold(NOW);
		clock.moveTo(NOW.plusSeconds(15));
		receiveTelemetryService.receive(telemetry(OBSERVED_AT.plusSeconds(15), 155, 1300));

		verify(recordNotificationPort).recordNotifications(any(NotificationRequest.class));
		verify(sessionEventPort).append(anyLong(), eq("PREPARE_NOTICE_SENT"), anyInt(), any(), any());
		assertThat(publishedTypes()).filteredOn("PREPARE_NOTICE_SENT"::equals).hasSize(1);
	}

	// ── 기준 밖 · 끝난 세션 ──

	@Test
	@DisplayName("차량 ETA 수신이 remainingMeters 701 · navEtaSeconds 181로 접근 중 · 준비 알림 기준 밖이면 ROUTING · stateVersion을 그대로 두고 VEHICLE_APPROACHING · PREPARE 알림 기록 요청 · PREPARE_NOTICE_SENT가 없다")
	void telemetryOutsideThresholdsCreatesNeitherApproachingNorPrepareNotice() {
		givenLockedSession(readyForCall().vehiclePhase(VehiclePhase.ROUTING).build());

		receiveTelemetryService.receive(telemetry(OBSERVED_AT, 181, 701));

		PickupSession saved = savedSession();
		assertThat(saved.getVehiclePhase()).isEqualTo(VehiclePhase.ROUTING);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION);
		verify(recordNotificationPort, never()).recordNotifications(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		assertThat(publishedTypes()).containsExactly("ETA_UPDATED");
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("끝난(COMPLETED · CANCELLED) 세션의 차량 ETA 수신은 접근 중 · 준비 알림 기준 안이어도 ROUTING · stateVersion을 그대로 두고 VEHICLE_APPROACHING · PREPARE 알림 기록 요청 · PREPARE_NOTICE_SENT가 없다")
	void telemetryOnFinishedSessionCreatesNeitherApproachingNorPrepareNotice(SessionStatus finished) {
		PickupSession session = readyForCall().sessionStatus(finished).vehiclePhase(VehiclePhase.ROUTING).build();
		givenLockedSession(session);

		receiveTelemetryService.receive(telemetry(OBSERVED_AT, 60, 650));

		assertThat(session.getVehiclePhase()).isEqualTo(VehiclePhase.ROUTING);
		assertThat(session.getStateVersion()).isEqualTo(STATE_VERSION);
		verify(recordNotificationPort, never()).recordNotifications(any());
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verify(publishSessionEventPort, never()).publish(any());
	}
	@Test
	@DisplayName("차량 ETA 수신은 SIG-01 location을 세션의 마지막 차량 위치 vehicleLocation으로, observedAt을 vehicleLocatedAt으로 저장한다")
	void telemetryStoresLocationAsVehicleLocationWithObservedAt() {
		givenLockedSession(readyForCall().build());

		receiveTelemetryService.receive(farFromSpot());

		PickupSession saved = savedSession();
		assertThat(saved.getVehicleLocation()).isEqualTo(VEHICLE_LOCATION);
		assertThat(saved.getVehicleLocatedAt()).isEqualTo(OBSERVED_AT);
	}

	@Test
	@DisplayName("차량 ETA 수신은 이미 저장된 차량 위치가 있으면 새 location과 observedAt으로 덮어쓴다")
	void telemetryOverwritesPreviousVehicleLocation() {
		givenLockedSession(readyForCall().vehicleLocation(PREVIOUS_VEHICLE_LOCATION, PREVIOUS_OBSERVED_AT).build());

		receiveTelemetryService.receive(farFromSpot());

		PickupSession saved = savedSession();
		assertThat(saved.getVehicleLocation()).isEqualTo(VEHICLE_LOCATION);
		assertThat(saved.getVehicleLocatedAt()).isEqualTo(OBSERVED_AT);
	}

	@Test
	@DisplayName("observedAt이 세션 etaUpdatedAt보다 오래된 차량 ETA 수신은 저장된 차량 위치와 vehicleLocatedAt을 바꾸지 않는다")
	void telemetryObservedBeforeEtaUpdatedAtKeepsVehicleLocation() {
		PickupSession session = readyForCall()
				.vehicleLocation(PREVIOUS_VEHICLE_LOCATION, PREVIOUS_OBSERVED_AT)
				.build();
		givenLockedSession(session);

		receiveTelemetryService.receive(telemetry(PREVIOUS_OBSERVED_AT.minusSeconds(5), 170, 650));

		verify(saveSessionPort, never()).save(any());
		assertThat(session.getVehicleLocation()).isEqualTo(PREVIOUS_VEHICLE_LOCATION);
		assertThat(session.getVehicleLocatedAt()).isEqualTo(PREVIOUS_OBSERVED_AT);
	}

	@Test
	@DisplayName("차량 ETA 수신의 location이 null이면 ETA는 덮어쓰고 저장된 차량 위치와 vehicleLocatedAt은 그대로 둔다")
	void telemetryWithoutLocationKeepsVehicleLocationButUpdatesEta() {
		givenLockedSession(readyForCall().vehicleLocation(PREVIOUS_VEHICLE_LOCATION, PREVIOUS_OBSERVED_AT).build());
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);

		receiveTelemetryService.receive(new TelemetryCommand(vehicle, SESSION_ID, OBSERVED_AT, 600, 4200, null, null,
				null, null));

		PickupSession saved = savedSession();
		assertThat(saved.getEtaUpdatedAt()).isEqualTo(OBSERVED_AT);
		assertThat(saved.getRemainingMeters()).isEqualTo(4200);
		assertThat(saved.getVehicleLocation()).isEqualTo(PREVIOUS_VEHICLE_LOCATION);
		assertThat(saved.getVehicleLocatedAt()).isEqualTo(PREVIOUS_OBSERVED_AT);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("끝난(COMPLETED · CANCELLED) 세션의 차량 ETA 수신은 저장된 차량 위치를 바꾸지 않는다")
	void telemetryOnFinishedSessionKeepsVehicleLocation(SessionStatus finished) {
		PickupSession session = readyForCall()
				.sessionStatus(finished)
				.vehicleLocation(PREVIOUS_VEHICLE_LOCATION, PREVIOUS_OBSERVED_AT)
				.build();
		givenLockedSession(session);

		receiveTelemetryService.receive(farFromSpot());

		assertThat(session.getVehicleLocation()).isEqualTo(PREVIOUS_VEHICLE_LOCATION);
		assertThat(session.getVehicleLocatedAt()).isEqualTo(PREVIOUS_OBSERVED_AT);
	}


	// ── 준비 ──

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	// 실내 준비 알림(700m · 180초)을 이미 보낸 세션. 선례: ConfirmRouteServiceTest.givenDepartureNoticeAlreadySent
	private void givenPrepareNoticeAlreadySent() {
		lenient().when(sessionEventPort.loadFirst(SESSION_ID, "PREPARE_NOTICE_SENT")).thenReturn(Optional.of(
				SessionEvent.restore(EVENT_ID_BASE + LAST_SEQ, SESSION_ID, LAST_SEQ,
						SessionEventType.PREPARE_NOTICE_SENT, STATE_VERSION, PREVIOUS_OBSERVED_AT.minusSeconds(30),
						Map.of())));
	}

	private void givenJoinCallMode(JoinCallMode mode) {
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(new FamilyInfo(FAMILY_ID, mode, 0));
	}

	// 철회되지 않은 호출(ACCEPTED · SENT · DELIVERED · ACKNOWLEDGED)
	private void givenLiveCall(Call call) {
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(call));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call));
	}

	// 가장 최근 호출이 철회된 상태
	private void givenRevokedLatestCall(Call call) {
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call));
	}

	private static Call previousCall(CallStatus status) {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(3));
		Instant sentAt = status == CallStatus.ACCEPTED ? null : acceptedAt.plusSeconds(1);
		Instant revokedAt = status == CallStatus.REVOKED ? NOW.minus(Duration.ofMinutes(1)) : null;
		String revokeReason = status == CallStatus.REVOKED ? "아이가 아직 준비 중" : null;
		return Call.restore(PREV_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, SPOT_VERSION,
				acceptedAt, sentAt, null, null, revokedAt, revokeReason);
	}

	private static SessionFixture readyForCall() {
		return new SessionFixture();
	}

	// 기본값은 AUTO 가족에서 트리거만 맞으면 호출이 나가는 세션: ACTIVE · CONFIRMED · G3 일치 · ETA 신선 · callNotBefore 없음 · callHeld false
	private static final class SessionFixture {

		private SessionStatus sessionStatus = SessionStatus.ACTIVE;
		private VehiclePhase vehiclePhase = VehiclePhase.APPROACHING;
		private SpotStatus spotStatus = SpotStatus.CONFIRMED;
		private int spotVersion = SPOT_VERSION;
		private Integer routeSpotVersion = SPOT_VERSION;
		private boolean callHeld = false;
		private Instant etaUpdatedAt = PREVIOUS_OBSERVED_AT;
		private LatLng vehicleLocation;
		private Instant vehicleLocatedAt;
		private List<SessionChild> children = List.of(
				child(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, null),
				child(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, null));

		SessionFixture sessionStatus(SessionStatus value) {
			this.sessionStatus = value;
			return this;
		}

		// 호출이 나가려면 G6(차량 정차)도 통과해야 해서 호출 생성을 보는 테스트는 STOPPED를 지정한다
		SessionFixture vehiclePhase(VehiclePhase value) {
			this.vehiclePhase = value;
			return this;
		}

		SessionFixture spotStatus(SpotStatus value) {
			this.spotStatus = value;
			return this;
		}

		SessionFixture spotVersion(int value) {
			this.spotVersion = value;
			return this;
		}

		SessionFixture routeSpotVersion(Integer value) {
			this.routeSpotVersion = value;
			return this;
		}

		SessionFixture callHeld(boolean value) {
			this.callHeld = value;
			return this;
		}

		SessionFixture etaUpdatedAt(Instant value) {
			this.etaUpdatedAt = value;
			return this;
		}

		SessionFixture children(List<SessionChild> value) {
			this.children = value;
			return this;
		}

		// 직전 SIG-01에서 저장된 마지막 차량 위치
		SessionFixture vehicleLocation(LatLng location, Instant locatedAt) {
			this.vehicleLocation = location;
			this.vehicleLocatedAt = locatedAt;
			return this;
		}

		PickupSession build() {
			Instant etaAt = etaUpdatedAt == null ? null : etaUpdatedAt.plusSeconds(PREVIOUS_NAV_ETA_SECONDS);
			Integer remainingMeters = etaUpdatedAt == null ? null : PREVIOUS_REMAINING_METERS;
			if (vehicleLocation != null) {
				return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
						FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, sessionStatus, vehiclePhase,
						spotStatus, STATE_VERSION, spotVersion, ROUTE_VERSION, routeSpotVersion, callHeld, etaAt,
						remainingMeters, etaUpdatedAt, null, null, null, null, null, null, null, null,
						vehicleLocation, vehicleLocatedAt, children);
			}
			return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
					FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, sessionStatus, vehiclePhase,
					spotStatus, STATE_VERSION, spotVersion, ROUTE_VERSION, routeSpotVersion, callHeld, etaAt,
					remainingMeters, etaUpdatedAt, null, null, null, null, null, null, null, null, children);
		}
	}

	private static SessionChild child(long sessionChildId, long childUserId, long scheduleId, ChildPhase phase,
			Instant callNotBefore) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, phase, 0, callNotBefore, null, null,
				null, null);
	}

	private static TelemetryCommand farFromSpot() {
		return telemetry(OBSERVED_AT, 600, 4200);
	}

	private static TelemetryCommand etaWithin3Minutes() {
		return telemetry(OBSERVED_AT, 170, 1500);
	}

	private static TelemetryCommand within700Meters() {
		return telemetry(OBSERVED_AT, 240, 650);
	}

	private static TelemetryCommand etaWithin3MinutesAndWithin700Meters() {
		return telemetry(OBSERVED_AT, 170, 650);
	}

	private static TelemetryCommand telemetry(Instant observedAt, int navEtaSeconds, int remainingMeters) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new TelemetryCommand(vehicle, SESSION_ID, observedAt, navEtaSeconds, remainingMeters,
				VEHICLE_LOCATION, 90, 24.5, "D");
	}

	// ── 확인 ──

	private void assertNoCallCreated() {
		verify(saveCallPort, never()).saveCall(any());
		verify(recordNotificationPort, never()).recordNotifications(any());
		verify(sessionEventPort, never()).append(anyLong(), eq("CALL_TRIGGERED"), anyInt(), any(), any());
	}

	// 저장된 세션을 차량이 SES-03으로 볼 때의 call.guard
	private CallGuardView guardViewOf(PickupSession session) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new SessionViewAssembler(PROPERTIES).assemble(session, vehicle, new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0),
				List.of(), Optional.empty(), clock.instant()).call().guard();
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

	private SessionEvent appendedEventOf(String type) {
		List<SessionEvent> matched = appendedEvents.stream()
				.filter(event -> event.getType().name().equals(type))
				.toList();
		assertThat(matched).as("저장한 %s 이벤트", type).hasSize(1);
		return matched.get(0);
	}

	private List<SessionEventMessage> publishedMessages() {
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		return captor.getAllValues();
	}

	private List<String> publishedTypes() {
		return publishedMessages().stream()
				.map(SessionEventMessage::type)
				.toList();
	}

	private SessionEventMessage publishedMessageOf(String type) {
		List<SessionEventMessage> matched = publishedMessages().stream()
				.filter(message -> type.equals(message.type()))
				.toList();
		assertThat(matched).as("발행한 %s 이벤트", type).hasSize(1);
		return matched.get(0);
	}

	private static final class MovableClock extends Clock {

		private Instant instant;

		MovableClock(Instant instant) {
			this.instant = instant;
		}

		void moveTo(Instant value) {
			this.instant = value;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return Clock.fixed(instant, zone);
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}

}

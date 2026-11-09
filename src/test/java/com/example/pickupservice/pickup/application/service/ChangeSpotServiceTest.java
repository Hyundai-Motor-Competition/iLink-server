package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotCommand;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
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
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChangeSpotServiceTest {

	// 한국 시간 2026-11-09(월) 17:40
	private static final Instant NOW = Instant.parse("2026-11-09T08:40:00Z");
	private static final SessionViewAssembler ASSEMBLER = new SessionViewAssembler(PickupProperties.defaults());
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long ACADEMY_ID = 55L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long BACK_GATE_SPOT_ID = 23L;
	private static final long LIVE_CALL_ID = 3001L;

	// 정문으로 한 번 확정(SES-04)한 뒤: CONFIRMED, spotVersion 0, routeVersion 1
	private static final int STATE_VERSION = 5;
	private static final int SPOT_VERSION = 0;
	private static final long LAST_SEQ = 4L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng BACK_STOP_POINT = new LatLng(37.500800, 127.040300);
	private static final LatLng BACK_MEETING_POINT = new LatLng(37.500750, 127.040250);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	private ChangeSpotService changeSpotService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, ASSEMBLER, clock);
		changeSpotService = new ChangeSpotService(loadSessionPort, loadSpotPort, saveSessionPort, loadFamilyPort,
				loadCallPort, sessionEventPort, publishSessionEventPort, sessionAccess, clock);
		lenient().when(loadSpotPort.loadSpot(FRONT_GATE_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(FRONT_GATE_SPOT_ID, FAMILY_ID, ACADEMY_ID, "정문 앞", FRONT_STOP_POINT,
						FRONT_MEETING_POINT)));
		lenient().when(loadSpotPort.loadSpot(BACK_GATE_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(BACK_GATE_SPOT_ID, FAMILY_ID, ACADEMY_ID, "후문 앞", BACK_STOP_POINT,
						BACK_MEETING_POINT)));
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

	@ParameterizedTest(name = "호출 status {0}")
	@EnumSource(value = CallStatus.class, names = {"ACCEPTED", "SENT", "DELIVERED"})
	@DisplayName("장소 변경은 철회되지 않은 호출(ACCEPTED 포함)이 있으면 CALL_ALREADY_SENT로 거절하고 아무것도 저장 · 발행하지 않는다")
	void changeSpotRejectsLiveCallWithCallAlreadySent(CallStatus liveStatus) {
		givenLockedSession(confirmedFrontGate(calledChildren()));
		givenCalls(liveCall(liveStatus));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@ParameterizedTest(name = "요청 spotVersion {0} · 서버 0")
	@ValueSource(ints = {1, 2})
	@DisplayName("장소 변경은 stateVersion이 같아도 요청 spotVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절하고 아무것도 저장 · 발행하지 않는다")
	void changeSpotRejectsDifferentSpotVersionWithConflictAndLatestView(int requestedSpotVersion) {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID, STATE_VERSION, requestedSpotVersion)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("STATE_VERSION_CONFLICT");
		assertThat(exception.session()).isInstanceOfSatisfying(SessionView.class, latest -> {
			assertThat(latest.sessionId()).isEqualTo(SESSION_ID);
			assertThat(latest.stateVersion()).isEqualTo(STATE_VERSION);
			assertThat(latest.spot().spotId()).isEqualTo(FRONT_GATE_SPOT_ID);
			assertThat(latest.spot().spotVersion()).isEqualTo(SPOT_VERSION);
		});
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("장소 변경은 마지막 호출이 철회(REVOKED)됐으면 거절하지 않고 세션을 새 장소 · SELECTED · spotVersion 1로 저장한다")
	void changeSpotAcceptsWhenLatestCallIsRevoked() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(revokedCall()));
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.empty());

		changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotId()).isEqualTo(BACK_GATE_SPOT_ID);
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.SELECTED);
		assertThat(saved.getSpotVersion()).isEqualTo(SPOT_VERSION + 1);
		verify(sessionEventPort).append(eq(SESSION_ID), eq("SPOT_CHANGED"), anyInt(), any(), any());
	}

	@Test
	@DisplayName("장소 변경이 성공하면 세션을 저장한 뒤 SPOT_CHANGED를 저장한 세션의 stateVersion · 지금 시각으로 한 번 저장한다")
	void changeSpotAppendsSpotChangedWithSavedStateVersion() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		PickupSession saved = savedSession();
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(saved.getSpotName()).isEqualTo("후문 앞");
		assertThat(saved.getStopPoint()).isEqualTo(BACK_STOP_POINT);
		assertThat(saved.getMeetingPoint()).isEqualTo(BACK_MEETING_POINT);
		InOrder inOrder = inOrder(saveSessionPort, sessionEventPort);
		inOrder.verify(saveSessionPort, atLeastOnce()).save(any(PickupSession.class));
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SPOT_CHANGED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsExactly("SPOT_CHANGED");
	}

	@Test
	@DisplayName("장소 변경은 저장한 SPOT_CHANGED를 append가 준 seq로 보호자와 세션 자녀에게 발행한다")
	void changeSpotPublishesSpotChangedToGuardianAndChildren() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		SessionEvent appended = appendedEventOf("SPOT_CHANGED");
		SessionEventMessage message = publishedMessageOf("SPOT_CHANGED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("장소 변경이 성공하면 바뀐 stateVersion · 새 장소 · SELECTED의 세션 뷰를 돌려준다")
	void changeSpotReturnsViewOfSavedSession() {
		givenLockedSession(confirmedFrontGate(waitingChildren()));

		SessionView view = changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID));

		assertThat(view.sessionId()).isEqualTo(SESSION_ID);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.status().spot()).isEqualTo(SpotStatus.SELECTED);
		assertThat(view.spot().spotId()).isEqualTo(BACK_GATE_SPOT_ID);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("장소 변경은 끝난(COMPLETED · CANCELLED) 세션이면 SESSION_ALREADY_FINISHED로 거절하고 아무것도 저장 · 발행하지 않는다")
	void changeSpotRejectsFinishedSessionWithSessionAlreadyFinished(SessionStatus finishedStatus) {
		givenLockedSession(finishedFrontGate(finishedStatus));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeSpotService.changeSpot(command(BACK_GATE_SPOT_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void givenCalls(Call call) {
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(call));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(call));
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

	private static PickupSession confirmedFrontGate(List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, 1, SPOT_VERSION, false,
				NOW.plus(Duration.ofMinutes(4)), 1200, NOW.minusSeconds(10), null, null, null, null, null, null, null, null,
				children);
	}

	// 정문으로 확정한 뒤 끝난(완료 · 취소) 세션. stateVersion · spotVersion은 요청과 같다
	private static PickupSession finishedFrontGate(SessionStatus finishedStatus) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, finishedStatus, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, SPOT_VERSION, 1, SPOT_VERSION, false,
				NOW.plus(Duration.ofMinutes(4)), 1200, NOW.minusSeconds(10), null, null, null, null, null, null, null, null,
				waitingChildren());
	}

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

	private static Call liveCall(CallStatus status) {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(2));
		Instant sentAt = status == CallStatus.ACCEPTED ? null : acceptedAt.plusSeconds(1);
		Instant deliveredAt = status == CallStatus.DELIVERED ? acceptedAt.plusSeconds(3) : null;
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, SPOT_VERSION,
				acceptedAt, sentAt, deliveredAt, null, null, null);
	}

	private static Call revokedCall() {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(5));
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.REVOKED,
				SPOT_VERSION, acceptedAt, acceptedAt.plusSeconds(1), null, null, acceptedAt.plusSeconds(60),
				"장소를 바꾸려고 철회");
	}

	private static ChangeSpotCommand command(long spotId) {
		return command(spotId, STATE_VERSION, SPOT_VERSION);
	}

	private static ChangeSpotCommand command(long spotId, int stateVersion, int spotVersion) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new ChangeSpotCommand(vehicle, SESSION_ID, spotId, stateVersion, spotVersion);
	}

}

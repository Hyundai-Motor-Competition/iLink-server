package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.OpenIncidentCommand;
import com.example.pickupservice.pickup.application.port.in.SessionIncidentView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.IncidentType;
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
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OpenIncidentServiceTest {

	// 한국 시간 2026-11-09(월) 18:12:10. 탑승 인식 뒤 민준이 타지 않은 것을 확인한 상황
	private static final Instant NOW = Instant.parse("2026-11-09T09:12:10Z");
	private static final SessionViewAssembler ASSEMBLER = new SessionViewAssembler(PickupProperties.defaults());
	private static final Instant BOARDING_DETECTED_AT = NOW.minusSeconds(15);
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	// 같은 가족이지만 이 세션에 없는 자녀
	private static final long OUTSIDE_CHILD_ID = 35L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long SPOT_ID = 21L;
	private static final int STATE_VERSION = 13;
	private static final long LAST_SEQ = 20L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

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

	private OpenIncidentService openIncidentService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, ASSEMBLER, clock);
		openIncidentService = new OpenIncidentService(loadSessionPort, saveSessionPort, loadFamilyPort, loadCallPort,
				sessionEventPort, publishSessionEventPort, sessionAccess, clock);
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
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(minjunNotBoardedSession()));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(minjunNotBoardedSession()));
	}

	@Test
	@DisplayName("탑승 예외는 INCIDENT_OPENED를 저장한 세션의 stateVersion · 지금 시각 · payload type CHILD_NOT_ON_BOARD · childUserId로 한 번 저장한다")
	void openIncidentAppendsIncidentOpenedWithTypeAndChildUserId() {
		openIncidentService.openIncident(command());

		PickupSession saved = savedSession();
		assertThat(appendedEvents).hasSize(1);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(appended.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(appended.getType()).isEqualTo(SessionEventType.INCIDENT_OPENED);
		assertThat(appended.getStateVersion()).isEqualTo(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(appended.getOccurredAt()).isEqualTo(NOW);
		Map<String, Object> payload = appended.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload.get("type")).as("payload type").hasToString("CHILD_NOT_ON_BOARD");
		assertThat(payload.get("childUserId")).as("payload childUserId").isEqualTo(MINJUN_ID);
	}

	@Test
	@DisplayName("탑승 예외는 세션을 ON_HOLD · incidentType CHILD_NOT_ON_BOARD · incidentOpenedAt 지금으로 저장한다")
	void openIncidentSavesSessionOnHoldWithIncident() {
		openIncidentService.openIncident(command());

		PickupSession saved = savedSession();
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.ON_HOLD);
		assertThat(saved.getIncidentType()).isEqualTo(IncidentType.CHILD_NOT_ON_BOARD);
		assertThat(saved.getIncidentOpenedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("탑승 예외는 대상 자녀를 세션의 incidentChildUserId로 저장한다")
	void openIncidentSavesIncidentChildUserIdOnSession() {
		openIncidentService.openIncident(command());

		PickupSession saved = savedSession();
		assertThat(saved.getIncidentChildUserId()).isEqualTo(MINJUN_ID);
	}

	@Test
	@DisplayName("탑승 예외는 돌려주는 세션 뷰의 incident에 type · childUserId · openedAt을 채운다")
	void openIncidentReturnsViewWithIncidentFilled() {
		SessionView view = openIncidentService.openIncident(command());

		assertThat(view.incident()).isEqualTo(new SessionIncidentView("CHILD_NOT_ON_BOARD", MINJUN_ID, NOW));
	}

	@Test
	@DisplayName("탑승 예외는 저장한 INCIDENT_OPENED를 append가 준 eventId · seq로 보호자와 세션 자녀에게 발행한다")
	void openIncidentPublishesIncidentOpenedToGuardianAndChildren() {
		openIncidentService.openIncident(command());

		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		List<SessionEventMessage> messages = captor.getAllValues();
		assertThat(messages).extracting(SessionEventMessage::type).containsExactly("INCIDENT_OPENED");
		SessionEventMessage message = messages.get(0);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(appended.getStateVersion());
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.payload()).isEqualTo(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("탑승 예외는 childUserId가 세션 자녀가 아니면(같은 가족 자녀여도) NOT_FOUND로 거절하고 아무것도 저장 · 발행하지 않는다")
	void openIncidentRejectsChildNotInSessionWithNotFound() {
		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> openIncidentService.openIncident(command(OUTSIDE_CHILD_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort, atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}

	// 정차 중 탑승 인식 뒤 SES-12로 민준의 boardedAt만 비운 세션
	private static PickupSession minjunNotBoardedSession() {
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, null, null,
						NOW.minusSeconds(90), null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, null, null,
						NOW.minusSeconds(80), BOARDING_DETECTED_AT));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, "해솔영어학원 정문 앞", STOP_POINT,
				MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.STOPPED, SpotStatus.CONFIRMED,
				STATE_VERSION, 0, 1, 0, false, NOW.minusSeconds(30), 0, NOW.minusSeconds(5), BOARDING_DETECTED_AT,
				BOARDING_DETECTED_AT.plusSeconds(10), BoardingDoor.REAR_RIGHT, null, null, null, null, null, children);
	}

	private static OpenIncidentCommand command() {
		return command(MINJUN_ID);
	}

	private static OpenIncidentCommand command(long childUserId) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new OpenIncidentCommand(vehicle, SESSION_ID, "CHILD_NOT_ON_BOARD", childUserId, STATE_VERSION);
	}

}

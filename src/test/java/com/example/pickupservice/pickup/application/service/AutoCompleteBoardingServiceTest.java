package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveOutcomePort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupOutcome;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoCompleteBoardingServiceTest {

	// 한국 시간 2026-11-09(월) 18:13:31 — 18:13:20 뒷문 탑승 인식, 취소 창(10초) 마감 18:13:30이 지남
	private static final Instant NOW = Instant.parse("2026-11-09T09:13:31Z");
	private static final Instant DOOR_AT = Instant.parse("2026-11-09T09:13:20Z");
	private static final Instant CANCELABLE_UNTIL = DOOR_AT.plusSeconds(10);
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;

	// SES-12로 한 자녀의 boardedAt이 비워진 다른 가족의 세션
	private static final long REJECTED_SESSION_ID = 502L;
	private static final long OTHER_VEHICLE_ID = 78L;
	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_GUARDIAN_ID = 41L;
	private static final long JIHO_ID = 43L;
	private static final long JIWOO_ID = 44L;
	private static final long JIHO_SCHEDULE_ID = 811L;
	private static final long JIWOO_SCHEDULE_ID = 812L;

	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final int STATE_VERSION = 15;
	private static final int SPOT_VERSION = 1;
	private static final long LAST_SEQ = 14L;
	private static final long EVENT_ID_BASE = 9000L;

	// 정차 직전 마지막 ETA 18:12:00, 실제 정차 18:12:40(SIG-02 STOPPED occurredAt). DB payload의 시각은 문자열
	private static final String PREDICTED_STOP_AT_JSON = "2026-11-09T09:12:00Z";
	private static final Instant PREDICTED_STOP_AT = Instant.parse(PREDICTED_STOP_AT_JSON);
	private static final Instant STOPPED_AT = Instant.parse("2026-11-09T09:12:40Z");
	private static final int STATE_VERSION_AT_STOP = 13;
	private static final long VEHICLE_STOPPED_SEQ = 12L;
	private static final Instant ACKNOWLEDGED_AT = Instant.parse("2026-11-09T09:09:30Z");

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SaveOutcomePort saveOutcomePort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	private AutoCompleteBoardingService autoCompleteBoardingService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		PickupCompletion pickupCompletion = new PickupCompletion(saveOutcomePort, sessionEventPort,
				publishSessionEventPort);
		autoCompleteBoardingService = new AutoCompleteBoardingService(loadSessionPort, saveSessionPort,
				loadFamilyPort, pickupCompletion);
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(loadFamilyPort.loadMembers(OTHER_FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(OTHER_GUARDIAN_ID, OTHER_FAMILY_ID, Role.GUARDIAN, "정보호", "010-9999-0000"),
				new MemberInfo(JIHO_ID, OTHER_FAMILY_ID, Role.CHILD, "지호", null),
				new MemberInfo(JIWOO_ID, OTHER_FAMILY_ID, Role.CHILD, "지우", null)));
		lenient().when(sessionEventPort.loadFirst(anyLong(), eq("VEHICLE_STOPPED")))
				.thenAnswer(invocation -> Optional.of(vehicleStopped(invocation.<Long>getArgument(0))));
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
	@DisplayName("자동 완료 작업은 취소 창이 지나고 모든 자녀가 탄 세션을 COMPLETED · stateVersion 1 올려 저장하고 완료한 세션 수 1을 돌려준다")
	void completeExpiredBoardingsSavesAllBoardedSessionAsCompletedAndReturnsCount() {
		givenExpired(allBoardedSession());
		givenLocked(SESSION_ID, allBoardedSession());

		int completed = autoCompleteBoardingService.completeExpiredBoardings(NOW);

		assertThat(completed).isEqualTo(1);
		PickupSession saved = savedSession();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(saved.getChildren()).allSatisfy(child -> {
			assertThat(child.getBoardedAt()).isEqualTo(DOOR_AT);
			assertThat(child.getLastLocation()).isNull();
			assertThat(child.getLastLocatedAt()).isNull();
		});
	}

	@Test
	@DisplayName("자동 완료 작업은 PickupOutcome을 boardingMethod DOOR · completedAt 지금, VEHICLE_STOPPED의 actualStopAt · predictedStopAt으로 저장한다")
	void completeExpiredBoardingsSavesOutcomeWithDoorMethod() {
		givenExpired(allBoardedSession());
		givenLocked(SESSION_ID, allBoardedSession());

		autoCompleteBoardingService.completeExpiredBoardings(NOW);

		ArgumentCaptor<PickupOutcome> captor = ArgumentCaptor.forClass(PickupOutcome.class);
		verify(saveOutcomePort).saveOutcome(captor.capture());
		PickupOutcome outcome = captor.getValue();
		assertThat(outcome.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(outcome.getBoardingMethod()).isEqualTo(BoardingMethod.DOOR);
		assertThat(outcome.getCompletedAt()).isEqualTo(NOW);
		assertThat(outcome.getActualStopAt()).isEqualTo(STOPPED_AT);
		assertThat(outcome.getPredictedStopAt()).isEqualTo(PREDICTED_STOP_AT);
	}

	@Test
	@DisplayName("자동 완료 작업은 SESSION_COMPLETED를 저장한 세션의 stateVersion · 지금 시각으로 저장하고 보호자와 세션 자녀에게 발행한다")
	void completeExpiredBoardingsAppendsAndPublishesSessionCompleted() {
		givenExpired(allBoardedSession());
		givenLocked(SESSION_ID, allBoardedSession());

		autoCompleteBoardingService.completeExpiredBoardings(NOW);

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SESSION_COMPLETED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents).extracting(event -> event.getType().name())
				.containsExactly("SESSION_COMPLETED");
		List<SessionEventMessage> messages = publishedMessages();
		assertThat(messages).hasSize(1);
		SessionEventMessage message = messages.get(0);
		SessionEvent appended = appendedEvents.get(0);
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.type()).isEqualTo("SESSION_COMPLETED");
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("자동 완료 작업은 SES-12로 boardedAt이 비워진 자녀가 있는 세션을 완료하지 않고 0을 돌려준다")
	void completeExpiredBoardingsSkipsSessionWithRejectedChild() {
		givenExpired(rejectedSession());
		givenLocked(REJECTED_SESSION_ID, rejectedSession());

		int completed = autoCompleteBoardingService.completeExpiredBoardings(NOW);

		assertThat(completed).isZero();
		verifyNothingCompleted();
	}

	@Test
	@DisplayName("자동 완료 작업은 목록에서는 모두 탔어도 잠가 다시 읽은 세션에 SES-12로 boardedAt이 비워진 자녀가 있으면 완료하지 않는다")
	void completeExpiredBoardingsRechecksLockedSessionBeforeCompleting() {
		givenExpired(allBoardedSession());
		givenLocked(SESSION_ID, sessionWithSeoyunRejected());

		int completed = autoCompleteBoardingService.completeExpiredBoardings(NOW);

		assertThat(completed).isZero();
		verify(loadSessionPort).loadForUpdate(SESSION_ID);
		verifyNothingCompleted();
	}

	@Test
	@DisplayName("자동 완료 작업은 SES-12로 boardedAt이 비워진 세션과 모두 탄 세션 중 모두 탄 세션만 완료하고 1을 돌려준다")
	void completeExpiredBoardingsCompletesOnlyAllBoardedSessionAmongMany() {
		givenExpired(rejectedSession(), allBoardedSession());
		givenLocked(REJECTED_SESSION_ID, rejectedSession());
		givenLocked(SESSION_ID, allBoardedSession());

		int completed = autoCompleteBoardingService.completeExpiredBoardings(NOW);

		assertThat(completed).isEqualTo(1);
		ArgumentCaptor<PickupSession> sessionCaptor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort, atLeastOnce()).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getAllValues()).extracting(PickupSession::getSessionId).containsOnly(SESSION_ID);
		ArgumentCaptor<PickupOutcome> outcomeCaptor = ArgumentCaptor.forClass(PickupOutcome.class);
		verify(saveOutcomePort).saveOutcome(outcomeCaptor.capture());
		assertThat(outcomeCaptor.getValue().getSessionId()).isEqualTo(SESSION_ID);
		verify(sessionEventPort, never())
				.append(eq(REJECTED_SESSION_ID), anyString(), anyInt(), any(Instant.class), any());
	}

	@Test
	@DisplayName("자동 완료 작업은 잠가 다시 읽은 세션이 이미 COMPLETED(그 사이 SES-11 완료)면 건너뛰고 0을 돌려준다")
	void completeExpiredBoardingsSkipsSessionAlreadyCompletedMeanwhile() {
		givenExpired(allBoardedSession());
		givenLocked(SESSION_ID, allBoardedSessionWithStatus(SessionStatus.COMPLETED, STATE_VERSION + 1));

		int completed = autoCompleteBoardingService.completeExpiredBoardings(NOW);

		assertThat(completed).isZero();
		verifyNothingCompleted();
	}

	private void givenExpired(PickupSession... sessions) {
		lenient().when(loadSessionPort.loadBoardingExpired(NOW)).thenReturn(List.of(sessions));
	}

	private void givenLocked(long sessionId, PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(sessionId)).thenReturn(Optional.of(session));
	}

	private void verifyNothingCompleted() {
		verify(saveSessionPort, never()).save(any(PickupSession.class));
		verify(saveOutcomePort, never()).saveOutcome(any(PickupOutcome.class));
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

	// 18:13:20 뒷문 열림으로 두 자녀 모두 탄 세션
	private static PickupSession allBoardedSession() {
		return allBoardedSessionWithStatus(SessionStatus.ACTIVE, STATE_VERSION);
	}

	private static PickupSession allBoardedSessionWithStatus(SessionStatus status, int stateVersion) {
		return session(SESSION_ID, VEHICLE_ID, FAMILY_ID, status, stateVersion, List.of(
				boardedChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, DOOR_AT),
				boardedChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, DOOR_AT)));
	}

	// 같은 세션인데 취소 창 안에 SES-12로 서윤의 boardedAt이 비워짐(stateVersion +1)
	private static PickupSession sessionWithSeoyunRejected() {
		return session(SESSION_ID, VEHICLE_ID, FAMILY_ID, SessionStatus.ACTIVE, STATE_VERSION + 1, List.of(
				boardedChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, DOOR_AT),
				boardedChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, null)));
	}

	// 다른 가족 세션: 지호는 탔고 지우는 SES-12로 boardedAt이 비워짐
	private static PickupSession rejectedSession() {
		return session(REJECTED_SESSION_ID, OTHER_VEHICLE_ID, OTHER_FAMILY_ID, SessionStatus.ACTIVE,
				STATE_VERSION + 1, List.of(
						boardedChild(7101L, JIHO_ID, JIHO_SCHEDULE_ID, DOOR_AT),
						boardedChild(7102L, JIWOO_ID, JIWOO_SCHEDULE_ID, null)));
	}

	private static SessionChild boardedChild(long sessionChildId, long childUserId, long scheduleId,
			Instant boardedAt) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.MEETING, 0, null,
				new LatLng(37.501350, 127.039920), DOOR_AT.minusSeconds(10), ACKNOWLEDGED_AT, boardedAt);
	}

	private static PickupSession session(long sessionId, long vehicleId, long familyId, SessionStatus status,
			int stateVersion, List<SessionChild> children) {
		return PickupSession.restore(sessionId, vehicleId, familyId, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, VehiclePhase.STOPPED,
				SpotStatus.CONFIRMED, stateVersion, SPOT_VERSION, 2, SPOT_VERSION, false, PREDICTED_STOP_AT, 0,
				STOPPED_AT, DOOR_AT, CANCELABLE_UNTIL, BoardingDoor.REAR_RIGHT, null, null, null, null, children);
	}

	private static SessionEvent vehicleStopped(long sessionId) {
		return SessionEvent.restore(EVENT_ID_BASE + VEHICLE_STOPPED_SEQ, sessionId, VEHICLE_STOPPED_SEQ,
				SessionEventType.VEHICLE_STOPPED, STATE_VERSION_AT_STOP, STOPPED_AT,
				Map.of("predictedStopAt", PREDICTED_STOP_AT_JSON));
	}

}

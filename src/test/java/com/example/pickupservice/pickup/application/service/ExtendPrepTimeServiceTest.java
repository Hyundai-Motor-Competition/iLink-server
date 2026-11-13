package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.ExtendPrepTimeCommand;
import com.example.pickupservice.pickup.application.port.in.PrepExtensionResult;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
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
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

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
import static org.mockito.Mockito.when;

// SIG-03 준비 시간 추가: 자녀 주체가 자기 준비 시간을 늘린다. 요청에 stateVersion이 없어 버전 확인은 하지 않는다
@ExtendWith(MockitoExtension.class)
class ExtendPrepTimeServiceTest {

	// 한국 시간 2026-11-09(월) 18:09 — 설계서 예시 「18:09에 +5분 → 18:14」
	private static final Instant NOW = Instant.parse("2026-11-09T09:09:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
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
	private static final long LIVE_CALL_ID = 3301L;
	private static final int STATE_VERSION = 8;
	private static final long LAST_SEQ = 6L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	@Mock
	private RecordNotificationPort recordNotificationPort;

	private ExtendPrepTimeService extendPrepTimeService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		extendPrepTimeService = new ExtendPrepTimeService(loadSessionPort, saveSessionPort, loadCallPort,
				loadFamilyPort, sessionEventPort, publishSessionEventPort, recordNotificationPort,
				PickupProperties.defaults(), Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
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

	static Stream<Arguments> calledChildren() {
		return Stream.of(
				Arguments.of(ChildPhase.CALLED, CallStatus.SENT),
				Arguments.of(ChildPhase.MEETING, CallStatus.DELIVERED));
	}

	@ParameterizedTest(name = "자녀 {0} · 호출 {1}")
	@MethodSource("calledChildren")
	@DisplayName("준비 시간 추가는 합류 안내를 이미 받은 자녀면 CALL_ALREADY_SENT로 거절한다 — 409를 주고 세션 · 이벤트 저장, 발행, 알림 기록 요청을 하지 않는다")
	void extendRejectsChildWhoAlreadyReceivedCallWithCallAlreadySent(ChildPhase minjunPhase, CallStatus callStatus) {
		givenLockedSession(calledSession(minjunPhase));
		givenLiveCall(callStatus);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> extendPrepTimeService.extend(minjunCommand(5)));

		assertCallAlreadySent(exception);
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("준비 시간 추가는 발송 전(ACCEPTED)이라도 철회되지 않은 호출이 있으면 CALL_ALREADY_SENT로 거절한다(사람 결정 2026-11-09 「이미 보냄」 = 철회되지 않은 호출, ACCEPTED 포함)")
	void extendRejectsWhenAcceptedCallIsLiveWithCallAlreadySent() {
		givenLockedSession(waitingSession(0, null));
		givenLiveCall(CallStatus.ACCEPTED);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> extendPrepTimeService.extend(minjunCommand(5)));

		assertCallAlreadySent(exception);
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("준비 시간 추가는 그 자녀의 extraPrepMinutes · callNotBefore(지금 + 누적 분)를 바꾸고 stateVersion을 1 올려 저장한다 — childPhase와 형제는 그대로")
	void extendSavesChildPrepTimeAndRaisesStateVersion() {
		givenLockedSession(waitingSession(0, null));

		extendPrepTimeService.extend(minjunCommand(5));

		PickupSession saved = savedSession();
		SessionChild minjun = childOf(saved, MINJUN_ID);
		assertThat(minjun.getExtraPrepMinutes()).as("민준 extraPrepMinutes").isEqualTo(5);
		assertThat(minjun.getCallNotBefore()).as("민준 callNotBefore 18:14")
				.isEqualTo(Instant.parse("2026-11-09T09:14:00Z"));
		assertThat(minjun.getChildPhase()).as("민준 childPhase").isEqualTo(ChildPhase.WAITING);
		SessionChild seoyun = childOf(saved, SEOYUN_ID);
		assertThat(seoyun.getExtraPrepMinutes()).as("서윤 extraPrepMinutes").isZero();
		assertThat(seoyun.getCallNotBefore()).as("서윤 callNotBefore").isNull();
		assertThat(saved.getStateVersion()).as("stateVersion").isEqualTo(STATE_VERSION + 1);
	}

	@Test
	@DisplayName("준비 시간 추가는 세션을 저장한 뒤 PREP_TIME_EXTENDED를 저장한 세션의 stateVersion으로 한 번 저장한다")
	void extendAppendsPrepTimeExtendedWithSavedStateVersion() {
		givenLockedSession(waitingSession(0, null));

		extendPrepTimeService.extend(minjunCommand(5));

		InOrder inOrder = inOrder(saveSessionPort, sessionEventPort);
		inOrder.verify(saveSessionPort, atLeastOnce()).save(any(PickupSession.class));
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("PREP_TIME_EXTENDED"), eq(STATE_VERSION + 1), eq(NOW), any());
		assertThat(appendedEvents).extracting(SessionEvent::getType)
				.containsExactly(SessionEventType.PREP_TIME_EXTENDED);
	}

	@Test
	@DisplayName("준비 시간 추가는 저장한 PREP_TIME_EXTENDED를 append가 준 seq로 보호자에게 발행한다(받는 쪽 차량 · 보호자)")
	void extendPublishesPrepTimeExtendedToGuardian() {
		givenLockedSession(waitingSession(0, null));

		extendPrepTimeService.extend(minjunCommand(5));

		SessionEvent appended = appendedEventOf("PREP_TIME_EXTENDED");
		SessionEventMessage message = publishedMessageOf("PREP_TIME_EXTENDED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		// 발행 payload에는 알림 기록이 돌려준 notificationIds가 더해져서 isEqualTo 대신 containsAllEntriesOf로 바꿨다
		assertThat(message.payload()).containsAllEntriesOf(appended.getPayload());
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("준비 시간 추가는 알림 기록이 돌려준 notificationId 목록을 알림 기록 뒤에 발행하는 PREP_TIME_EXTENDED payload의 notificationIds에 담고 저장하는 이벤트 payload에는 넣지 않는다")
	void extendPublishesPrepTimeExtendedWithRecordedNotificationIds() {
		givenLockedSession(waitingSession(0, null));
		when(recordNotificationPort.recordNotifications(any(NotificationRequest.class)))
				.thenReturn(List.of(7001L, 7002L));

		extendPrepTimeService.extend(minjunCommand(5));

		InOrder inOrder = inOrder(recordNotificationPort, publishSessionEventPort);
		inOrder.verify(recordNotificationPort).recordNotifications(any(NotificationRequest.class));
		inOrder.verify(publishSessionEventPort).publish(any(SessionEventMessage.class));
		SessionEventMessage message = publishedMessageOf("PREP_TIME_EXTENDED");
		assertThat(message.payload()).containsEntry("notificationIds", List.of(7001L, 7002L))
				.containsAllEntriesOf(appendedEventOf("PREP_TIME_EXTENDED").getPayload());
		assertThat(appendedEventOf("PREP_TIME_EXTENDED").getPayload()).doesNotContainKey("notificationIds");
	}

	@Test
	@DisplayName("PREP_TIME_EXTENDED payload의 callNotBefore는 저장 · 발행 모두 TimeUtils.toKst와 같은 +09:00 문자열이다")
	void prepTimeExtendedPayloadCallNotBeforeIsKstOffsetString() {
		givenLockedSession(waitingSession(0, null));

		extendPrepTimeService.extend(minjunCommand(5));

		String expected = TimeUtils.toKst(Instant.parse("2026-11-09T09:14:00Z"));
		assertThat(expected).isEqualTo("2026-11-09T18:14:00+09:00");
		assertThat(appendedEventOf("PREP_TIME_EXTENDED").getPayload()).containsEntry("callNotBefore", expected);
		assertThat(publishedMessageOf("PREP_TIME_EXTENDED").payload()).containsEntry("callNotBefore", expected);
	}

	@Test
	@DisplayName("준비 시간 추가는 보호자에게 PREP_EXTENDED 알림 기록을 한 번 요청한다 — 호출 전이라 callId는 null")
	void extendRequestsPrepExtendedNotificationForGuardian() {
		givenLockedSession(waitingSession(0, null));

		extendPrepTimeService.extend(minjunCommand(5));

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(captor.capture());
		NotificationRequest request = captor.getValue();
		assertThat(request.sessionId()).isEqualTo(SESSION_ID);
		assertThat(request.callId()).isNull();
		assertThat(request.type()).isEqualTo("PREP_EXTENDED");
		assertThat(request.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("PREP_EXTENDED 알림 기록과 PREP_TIME_EXTENDED 발행의 받는 사람은 가족의 GUARDIAN · CO_GUARDIAN · DELEGATE 모두이고 TEACHER · CHILD는 빠진다")
	void extendNotifiesGuardianCoGuardianAndDelegateWithoutTeacher() {
		givenAllRoleMembers();
		givenLockedSession(waitingSession(0, null));

		extendPrepTimeService.extend(minjunCommand(5));

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(captor.capture());
		assertThat(captor.getValue().receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, CO_GUARDIAN_ID, DELEGATE_ID);
		assertThat(publishedMessageOf("PREP_TIME_EXTENDED").receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, CO_GUARDIAN_ID, DELEGATE_ID);
	}

	private void givenAllRoleMembers() {
		when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444"),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666"),
				new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "최선생", "010-7777-8888"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
	}

	@Test
	@DisplayName("준비 시간 추가는 누적 extraPrepMinutes와 callNotBefore(지금 + 누적 분)를 돌려준다 — 이미 5분을 늘린 자녀가 10분을 더하면 15분 · 18:24")
	void extendReturnsAccumulatedMinutesAndCallNotBefore() {
		givenLockedSession(waitingSession(5, Instant.parse("2026-11-09T09:07:00Z")));

		PrepExtensionResult result = extendPrepTimeService.extend(minjunCommand(10));

		assertThat(result.extraPrepMinutes()).isEqualTo(15);
		assertThat(result.callNotBefore()).isEqualTo(Instant.parse("2026-11-09T09:24:00Z"));
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("준비 시간 추가는 끝난(COMPLETED · CANCELLED) 세션이면 SESSION_ALREADY_FINISHED로 거절한다 — 409를 주고 세션 · 이벤트 저장, 발행, 알림 기록 요청을 하지 않는다")
	void extendRejectsFinishedSessionWithSessionAlreadyFinished(SessionStatus finishedStatus) {
		givenLockedSession(finishedWaitingSession(finishedStatus));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> extendPrepTimeService.extend(minjunCommand(5)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	// 세션을 바꾸는 요청이므로 행 잠금(loadForUpdate)으로 읽는다(설계서 「세션 행 잠금 범위」)
	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void givenLiveCall(CallStatus status) {
		Instant sentAt = status == CallStatus.ACCEPTED ? null : NOW.minusSeconds(40);
		Instant deliveredAt = status == CallStatus.DELIVERED ? NOW.minusSeconds(35) : null;
		Call live = Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, status, 0,
				NOW.minusSeconds(45), sentAt, deliveredAt, null, null, null);
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(live));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(live));
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

	private static SessionChild childOf(PickupSession session, long childUserId) {
		return session.getChildren().stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.findFirst()
				.orElseThrow();
	}

	// 호출 전 두 자녀가 실내에서 기다리는 세션. 민준은 이미 minjunExtraMinutes만큼 늘렸을 수 있다
	private static PickupSession waitingSession(int minjunExtraMinutes, Instant minjunCallNotBefore) {
		return session(List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, minjunExtraMinutes,
						minjunCallNotBefore, null, null, null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null)));
	}

	// 합류 안내를 받은 세션. MEETING이면 민준이 호출을 확인한 상태
	private static PickupSession calledSession(ChildPhase minjunPhase) {
		Instant minjunAcknowledgedAt = minjunPhase == ChildPhase.MEETING ? NOW.minusSeconds(30) : null;
		return session(List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, minjunPhase, 0, null, null, null,
						minjunAcknowledgedAt, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null)));
	}

	// 호출 전 두 자녀가 기다리다 끝난(완료 · 취소) 세션
	private static PickupSession finishedWaitingSession(SessionStatus finishedStatus) {
		return session(finishedStatus, List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null)));
	}

	private static PickupSession session(List<SessionChild> children) {
		return session(SessionStatus.ACTIVE, children);
	}

	private static PickupSession session(SessionStatus status, List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, NOW.plus(Duration.ofMinutes(4)), 1500,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, children);
	}

	private static ExtendPrepTimeCommand minjunCommand(int minutes) {
		AuthPrincipal minjun = new AuthPrincipal(PrincipalType.USER, MINJUN_ID, FAMILY_ID, Role.CHILD);
		return new ExtendPrepTimeCommand(minjun, SESSION_ID, minutes);
	}

	private static void assertCallAlreadySent(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
	}

}

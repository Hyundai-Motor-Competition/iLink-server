package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.HelpRequestCommand;
import com.example.pickupservice.pickup.application.port.in.HelpRequestResult;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;
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
import static org.mockito.Mockito.when;

// SIG-05 도움 요청: 자녀 주체가 보낸다. 세션 상태를 바꾸지 않아 세션 저장 포트를 받지 않고 지금 stateVersion으로 이벤트를 남긴다
@ExtendWith(MockitoExtension.class)
class RequestHelpServiceTest {

	// 한국 시간 2026-11-09(월) 18:09
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
	private static final int STATE_VERSION = 11;
	private static final long LAST_SEQ = 9L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	@Mock
	private RecordNotificationPort recordNotificationPort;

	private RequestHelpService requestHelpService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		requestHelpService = new RequestHelpService(loadSessionPort, loadFamilyPort, sessionEventPort,
				publishSessionEventPort, recordNotificationPort, PickupProperties.defaults(),
				Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(calledSession()));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(sessionEventPort.loadLastHelpRequest(eq(SESSION_ID), anyLong())).thenReturn(Optional.empty());
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

	@ParameterizedTest(name = "type {0}")
	@ValueSource(strings = {"NEED_HELP", "CANT_FIND_CAR", "CONTACT_PARENT"})
	@DisplayName("도움 요청은 HELP_REQUESTED를 payload childUserId · type과 지금 stateVersion으로 한 번 저장한다")
	void requestAppendsHelpRequestedWithChildUserIdAndType(String type) {
		requestHelpService.request(command(MINJUN_ID, type));

		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("HELP_REQUESTED"), eq(STATE_VERSION), eq(NOW), any());
		SessionEvent appended = appendedEventOf("HELP_REQUESTED");
		assertThat(appended.getPayload()).containsOnly(entry("childUserId", MINJUN_ID), entry("type", type));
		assertThat(appendedEvents).hasSize(1);
	}

	@Test
	@DisplayName("도움 요청은 저장한 HELP_REQUESTED를 append가 준 seq로 보호자에게 발행한다(받는 쪽 차량 · 보호자)")
	void requestPublishesHelpRequestedToGuardian() {
		requestHelpService.request(command(MINJUN_ID, "CANT_FIND_CAR"));

		SessionEvent appended = appendedEventOf("HELP_REQUESTED");
		SessionEventMessage message = publishedMessageOf("HELP_REQUESTED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(STATE_VERSION);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		// 발행 payload에는 알림 기록이 돌려준 notificationIds가 더해져서 containsOnly 대신 containsEntry로 바꿨다
		assertThat(message.payload()).containsEntry("childUserId", MINJUN_ID).containsEntry("type", "CANT_FIND_CAR");
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("도움 요청은 알림 기록이 돌려준 notificationId 목록을 알림 기록 뒤에 발행하는 HELP_REQUESTED payload의 notificationIds에 담고 저장하는 이벤트 payload에는 넣지 않는다")
	void requestPublishesHelpRequestedWithRecordedNotificationIds() {
		when(recordNotificationPort.recordNotifications(any(NotificationRequest.class)))
				.thenReturn(List.of(7001L, 7002L));

		requestHelpService.request(command(MINJUN_ID, "NEED_HELP"));

		InOrder inOrder = inOrder(recordNotificationPort, publishSessionEventPort);
		inOrder.verify(recordNotificationPort).recordNotifications(any(NotificationRequest.class));
		inOrder.verify(publishSessionEventPort).publish(any(SessionEventMessage.class));
		SessionEventMessage message = publishedMessageOf("HELP_REQUESTED");
		assertThat(message.payload())
				.containsEntry("childUserId", MINJUN_ID)
				.containsEntry("type", "NEED_HELP")
				.containsEntry("notificationIds", List.of(7001L, 7002L));
		assertThat(appendedEventOf("HELP_REQUESTED").getPayload()).doesNotContainKey("notificationIds");
	}

	@Test
	@DisplayName("도움 요청은 보호자에게 HELP 알림 기록을 한 번 요청한다")
	void requestRecordsHelpNotificationForGuardian() {
		requestHelpService.request(command(MINJUN_ID, "NEED_HELP"));

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(captor.capture());
		NotificationRequest request = captor.getValue();
		assertThat(request.sessionId()).isEqualTo(SESSION_ID);
		assertThat(request.callId()).isNull();
		assertThat(request.type()).isEqualTo("HELP");
		assertThat(request.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("HELP 알림 기록과 HELP_REQUESTED 발행의 받는 사람은 가족의 GUARDIAN · CO_GUARDIAN · DELEGATE 모두이고 TEACHER · CHILD는 빠진다")
	void requestNotifiesGuardianCoGuardianAndDelegateWithoutTeacher() {
		when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444"),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666"),
				new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "최선생", "010-7777-8888"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));

		requestHelpService.request(command(MINJUN_ID, "NEED_HELP"));

		ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(captor.capture());
		assertThat(captor.getValue().receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, CO_GUARDIAN_ID, DELEGATE_ID);
		assertThat(publishedMessageOf("HELP_REQUESTED").receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, CO_GUARDIAN_ID, DELEGATE_ID);
	}

	static Stream<Arguments> acceptedLastRequests() {
		return Stream.of(
				Arguments.of(Named.of("첫 요청", Optional.empty())),
				Arguments.of(Named.of("61초 전에 요청한 뒤", Optional.of(NOW.minusSeconds(61)))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedLastRequests")
	@DisplayName("도움 요청은 받으면 nextAllowedAt = 지금 + 60초(18:10)를 돌려준다 — 마지막 요청 시각 + 60초가 아니다")
	void requestReturnsNextAllowedAtSixtySecondsFromNow(Optional<Instant> minjunLastRequestedAt) {
		minjunLastRequestedAt.ifPresent(at -> givenLastHelpRequest(MINJUN_ID, at));

		HelpRequestResult result = requestHelpService.request(command(MINJUN_ID, "NEED_HELP"));

		assertThat(result.nextAllowedAt()).isEqualTo(Instant.parse("2026-11-09T09:10:00Z"));
	}

	@Test
	@DisplayName("도움 요청은 같은 아이가 60초 안에 다시 보내면 429 HELP_REQUEST_TOO_SOON과 nextAllowedAt(마지막 시각 + 60초)으로 거절하고 저장 · 발행 · 알림 기록 요청을 하지 않는다")
	void requestRejectsSameChildWithinSixtySecondsWithHelpRequestTooSoon() {
		givenLastHelpRequest(MINJUN_ID, NOW.minusSeconds(30));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> requestHelpService.request(command(MINJUN_ID, "NEED_HELP")));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exception.code()).isEqualTo("HELP_REQUEST_TOO_SOON");
		assertThat(exception.details())
				.containsOnly(entry("nextAllowedAt", Instant.parse("2026-11-09T09:09:30Z")));
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verifyNoInteractions(publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("도움 요청 60초 안 재요청의 HELP_REQUEST_TOO_SOON은 retryAfterSeconds 30(nextAllowedAt 18:09:30 - 지금 18:09:00)을 담고 추가 키 nextAllowedAt도 그대로 담는다")
	void requestTooSoonCarriesRetryAfterSecondsAndKeepsNextAllowedAt() {
		givenLastHelpRequest(MINJUN_ID, NOW.minusSeconds(30));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> requestHelpService.request(command(MINJUN_ID, "NEED_HELP")));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exception.code()).isEqualTo("HELP_REQUEST_TOO_SOON");
		assertThat(exception.retryAfterSeconds()).isEqualTo(30);
		assertThat(exception.details())
				.containsOnly(entry("nextAllowedAt", Instant.parse("2026-11-09T09:09:30Z")));
	}

	@Test
	@DisplayName("도움 요청의 retryAfterSeconds는 남은 시간을 초 단위로 올림한다 — 마지막 요청 30.5초 뒤면 29.5초 남아 30")
	void requestTooSoonRoundsRetryAfterSecondsUp() {
		givenLastHelpRequest(MINJUN_ID, NOW.minusMillis(30_500));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> requestHelpService.request(command(MINJUN_ID, "NEED_HELP")));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("HELP_REQUEST_TOO_SOON");
		assertThat(exception.retryAfterSeconds()).isEqualTo(30);
	}

	@Test
	@DisplayName("도움 요청 1분 제한은 형제의 요청에 영향을 받지 않는다 — 민준이 10초 전에 요청했어도 서윤의 요청은 받아 서윤의 HELP_REQUESTED를 저장하고 지금 + 60초를 돌려준다")
	void requestIsNotLimitedBySiblingRecentRequest() {
		givenLastHelpRequest(MINJUN_ID, NOW.minusSeconds(10));

		HelpRequestResult result = requestHelpService.request(command(SEOYUN_ID, "CANT_FIND_CAR"));

		assertThat(result.nextAllowedAt()).isEqualTo(Instant.parse("2026-11-09T09:10:00Z"));
		SessionEvent appended = appendedEventOf("HELP_REQUESTED");
		assertThat(appended.getPayload()).containsOnly(entry("childUserId", SEOYUN_ID), entry("type", "CANT_FIND_CAR"));
		verify(sessionEventPort).loadLastHelpRequest(SESSION_ID, SEOYUN_ID);
		verify(recordNotificationPort).recordNotifications(any(NotificationRequest.class));
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("도움 요청은 끝난(COMPLETED · CANCELLED) 세션이면 409 SESSION_ALREADY_FINISHED로 거절하고 이벤트 저장 · 발행 · 알림 기록 요청을 하지 않는다")
	void requestRejectsFinishedSessionWithSessionAlreadyFinished(SessionStatus finishedStatus) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(finishedSession(finishedStatus)));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> requestHelpService.request(command(MINJUN_ID, "NEED_HELP")));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verifyNoInteractions(publishSessionEventPort, recordNotificationPort);
	}

	// loadLastHelpRequest는 그 아이(payload childUserId)의 마지막 HELP_REQUESTED만 돌려준다
	private void givenLastHelpRequest(long childUserId, Instant requestedAt) {
		SessionEvent last = SessionEvent.restore(EVENT_ID_BASE + LAST_SEQ, SESSION_ID, LAST_SEQ,
				SessionEventType.HELP_REQUESTED, STATE_VERSION, requestedAt,
				Map.of("childUserId", childUserId, "type", "NEED_HELP"));
		lenient().when(sessionEventPort.loadLastHelpRequest(SESSION_ID, childUserId)).thenReturn(Optional.of(last));
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

	// 합류 안내를 받고 두 자녀가 차로 오는 중인 세션(「차를 못 찾고 있어요」가 나올 수 있는 때)
	private static PickupSession calledSession() {
		return finishedSession(SessionStatus.ACTIVE);
	}

	// calledSession과 같은 세션을 주어진 상태로(끝난 상태 포함) 만든다
	private static PickupSession finishedSession(SessionStatus status) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, VehiclePhase.STOPPED,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, NOW.minusSeconds(20), 0,
				NOW.minusSeconds(5), null, null, null, null, null, null, null, null, List.of(
						SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null,
								null, null, null),
						SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null,
								null, null, null)));
	}

	private static HelpRequestCommand command(long childUserId, String type) {
		AuthPrincipal child = new AuthPrincipal(PrincipalType.USER, childUserId, FAMILY_ID, Role.CHILD);
		return new HelpRequestCommand(child, SESSION_ID, type);
	}

}

package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteCommand;
import com.example.pickupservice.pickup.application.port.in.SpotCandidate;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveTemporarySpotPort;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmRouteServiceTest {

	// 한국 시간 2026-11-09(월) 17:30
	private static final Instant NOW = Instant.parse("2026-11-09T08:30:00Z");
	private static final SessionViewAssembler ASSEMBLER = new SessionViewAssembler(PickupProperties.defaults());
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long OTHER_VEHICLE_ID = 78L;
	private static final long FAMILY_ID = 12L;
	private static final long OTHER_FAMILY_ID = 13L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long ACADEMY_ID = 55L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long BACK_GATE_SPOT_ID = 23L;
	private static final long OTHER_FAMILY_SPOT_ID = 24L;
	private static final long UNKNOWN_SPOT_ID = 999L;
	private static final String NAV_ROUTE_ID = "nav-route-1";

	// 첫 확정 전 세션: SES-02 뒤 SELECTED, routeVersion 0
	private static final int FIRST_STATE_VERSION = 3;
	// 두 번째 확정 전 세션: 한 번 확정한 뒤 SES-05로 후문으로 바꿔 SELECTED, spotVersion 1, routeVersion 1
	private static final int CHANGED_STATE_VERSION = 6;
	private static final long LAST_SEQ = 4L;
	private static final long EVENT_ID_BASE = 9000L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng BACK_STOP_POINT = new LatLng(37.500800, 127.040300);
	private static final LatLng BACK_MEETING_POINT = new LatLng(37.500750, 127.040250);

	// 추천 장소 candidate(#269): 경로 확정 때 임시 장소로 만든다
	private static final long TEMPORARY_SPOT_ID = 30L;
	private static final String CANDIDATE_NAME = "학원 옆 편의점 앞";
	private static final LatLng CANDIDATE_LOCATION = new LatLng(37.500900, 127.040500);
	private static final int CANDIDATE_TOTAL_METERS = 3_800;
	private static final long LIVE_CALL_ID = 6001L;

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private SaveTemporarySpotPort saveTemporarySpotPort;

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

	@Mock
	private RecordNotificationPort recordNotificationPort;

	private ConfirmRouteService confirmRouteService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, ASSEMBLER, clock);
		confirmRouteService = new ConfirmRouteService(loadSessionPort, loadSpotPort, saveTemporarySpotPort,
				saveSessionPort, loadFamilyPort, loadCallPort, sessionEventPort, publishSessionEventPort, recordNotificationPort, sessionAccess,
				clock);
		lenient().when(loadSpotPort.loadSpot(FRONT_GATE_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(FRONT_GATE_SPOT_ID, FAMILY_ID, ACADEMY_ID, "정문 앞", FRONT_STOP_POINT,
						FRONT_MEETING_POINT)));
		lenient().when(loadSpotPort.loadSpot(BACK_GATE_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(BACK_GATE_SPOT_ID, FAMILY_ID, ACADEMY_ID, "후문 앞", BACK_STOP_POINT,
						BACK_MEETING_POINT)));
		lenient().when(loadSpotPort.loadSpot(OTHER_FAMILY_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(OTHER_FAMILY_SPOT_ID, OTHER_FAMILY_ID, ACADEMY_ID, "다른 가족 정문", FRONT_STOP_POINT,
						FRONT_MEETING_POINT)));
		lenient().when(loadSpotPort.loadSpot(UNKNOWN_SPOT_ID)).thenReturn(Optional.empty());
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(familyMembers());
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

	@Test
	@DisplayName("경로 확정은 어느 가족도 저장하지 않은 spotId면 SPOT_NOT_SAVED로 거절하고 아무것도 저장하지 않는다")
	void confirmRouteRejectsUnknownSpotWithSpotNotSaved() {
		givenLockedSession(beforeFirstConfirm());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(command(UNKNOWN_SPOT_ID, FIRST_STATE_VERSION)));

		assertSpotNotSaved(exception);
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("경로 확정은 다른 가족이 저장한 spotId면 SPOT_NOT_SAVED로 거절하고 아무것도 저장하지 않는다")
	void confirmRouteRejectsSpotSavedByOtherFamilyWithSpotNotSaved() {
		givenLockedSession(beforeFirstConfirm());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(command(OTHER_FAMILY_SPOT_ID, FIRST_STATE_VERSION)));

		assertSpotNotSaved(exception);
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("첫 경로 확정은 세션 자녀 모두에게 DEPARTURE 알림 기록을 한 번 요청한다")
	void firstConfirmRouteRequestsDepartureNotificationForSessionChildren() {
		givenLockedSession(beforeFirstConfirm());

		confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION));

		ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(requestCaptor.capture());
		NotificationRequest request = requestCaptor.getValue();
		assertThat(request.sessionId()).isEqualTo(SESSION_ID);
		assertThat(request.callId()).isNull();
		assertThat(request.type()).isEqualTo("DEPARTURE");
		assertThat(request.receiverUserIds()).containsExactlyInAnyOrder(MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("첫 경로 확정은 세션을 저장한 뒤 SPOT_CONFIRMED 다음에 DEPARTURE_NOTICE_SENT를 저장한 세션의 stateVersion으로 저장한다")
	void firstConfirmRouteAppendsDepartureNoticeSentAfterSpotConfirmed() {
		givenLockedSession(beforeFirstConfirm());

		confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION));

		int savedStateVersion = FIRST_STATE_VERSION + 1;
		InOrder inOrder = inOrder(saveSessionPort, sessionEventPort);
		inOrder.verify(saveSessionPort, atLeastOnce()).save(any(PickupSession.class));
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SPOT_CONFIRMED"), eq(savedStateVersion), eq(NOW), any());
		inOrder.verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("DEPARTURE_NOTICE_SENT"), eq(savedStateVersion), eq(NOW), any());
		verify(sessionEventPort).append(anyLong(), eq("DEPARTURE_NOTICE_SENT"), anyInt(), any(), any());
	}

	@Test
	@DisplayName("첫 경로 확정은 저장한 DEPARTURE_NOTICE_SENT를 append가 준 seq로 보호자에게 발행한다")
	void firstConfirmRoutePublishesDepartureNoticeSentToGuardian() {
		givenLockedSession(beforeFirstConfirm());

		confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION));

		SessionEvent appended = appendedEventOf("DEPARTURE_NOTICE_SENT");
		SessionEventMessage message = publishedMessageOf("DEPARTURE_NOTICE_SENT");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(FIRST_STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("첫 경로 확정은 알림 기록이 돌려준 notificationId 목록을 알림 기록 뒤에 발행하는 DEPARTURE_NOTICE_SENT payload의 notificationIds에 담고 저장하는 이벤트 payload에는 넣지 않는다")
	void firstConfirmRoutePublishesDepartureNoticeSentWithRecordedNotificationIds() {
		givenLockedSession(beforeFirstConfirm());
		when(recordNotificationPort.recordNotifications(any(NotificationRequest.class)))
				.thenReturn(List.of(7001L, 7002L));

		confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION));

		InOrder inOrder = inOrder(recordNotificationPort, publishSessionEventPort);
		inOrder.verify(recordNotificationPort).recordNotifications(any(NotificationRequest.class));
		inOrder.verify(publishSessionEventPort)
				.publish(argThat(message -> "DEPARTURE_NOTICE_SENT".equals(message.type())));
		SessionEventMessage message = publishedMessageOf("DEPARTURE_NOTICE_SENT");
		assertThat(message.payload()).containsEntry("notificationIds", List.of(7001L, 7002L));
		assertThat(appendedEventOf("DEPARTURE_NOTICE_SENT").getPayload()).doesNotContainKey("notificationIds");
	}

	@Test
	@DisplayName("두 번째 경로 확정은 SPOT_CONFIRMED만 저장하고 DEPARTURE 알림 기록 요청 · DEPARTURE_NOTICE_SENT 저장 · 발행을 다시 하지 않는다")
	void secondConfirmRouteDoesNotRequestDepartureNotificationAgain() {
		givenLockedSession(afterSpotChanged());
		givenDepartureNoticeAlreadySent();

		confirmRouteService.confirmRoute(command(BACK_GATE_SPOT_ID, CHANGED_STATE_VERSION));

		verify(sessionEventPort).append(eq(SESSION_ID), eq("SPOT_CONFIRMED"), anyInt(), any(), any());
		verify(recordNotificationPort, never()).recordNotifications(any());
		verify(sessionEventPort, never()).append(anyLong(), eq("DEPARTURE_NOTICE_SENT"), anyInt(), any(), any());
		assertThat(publishedMessages())
				.extracting(SessionEventMessage::type)
				.doesNotContain("DEPARTURE_NOTICE_SENT");
	}

	@Test
	@DisplayName("첫 경로 확정은 세션을 CONFIRMED로 저장하고 SPOT_CONFIRMED를 저장한 세션의 stateVersion으로 한 번 저장한다")
	void firstConfirmRouteAppendsSpotConfirmedWithSavedStateVersion() {
		givenLockedSession(beforeFirstConfirm());

		confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(saved.getRouteVersion()).isEqualTo(1);
		assertThat(saved.getStateVersion()).isEqualTo(FIRST_STATE_VERSION + 1);
		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SPOT_CONFIRMED"), eq(FIRST_STATE_VERSION + 1), eq(NOW), any());
	}

	@Test
	@DisplayName("두 번째 경로 확정도 SPOT_CONFIRMED를 저장한 세션의 stateVersion으로 한 번 저장한다")
	void secondConfirmRouteAppendsSpotConfirmedWithSavedStateVersion() {
		givenLockedSession(afterSpotChanged());
		givenDepartureNoticeAlreadySent();

		confirmRouteService.confirmRoute(command(BACK_GATE_SPOT_ID, CHANGED_STATE_VERSION));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(saved.getRouteVersion()).isEqualTo(2);
		verify(sessionEventPort)
				.append(eq(SESSION_ID), eq("SPOT_CONFIRMED"), eq(CHANGED_STATE_VERSION + 1), eq(NOW), any());
	}

	@Test
	@DisplayName("경로 확정은 요청의 totalMeters를 세션 routeTotalMeters에 저장하고 돌려주는 세션 뷰 route.totalMeters에 그 값을 담는다")
	void confirmRouteStoresTotalMetersAndShowsItInRouteView() {
		givenLockedSession(beforeFirstConfirm());

		SessionView view = confirmRouteService.confirmRoute(
				commandWithTotalMeters(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION, 4_200));

		assertThat(savedSession().getRouteTotalMeters()).isEqualTo(4_200);
		assertThat(view.route().totalMeters()).isEqualTo(4_200);
		assertThat(view.route().routeVersion()).isEqualTo(1);
	}

	@Test
	@DisplayName("경로 확정 요청에 totalMeters가 없으면 세션 routeTotalMeters와 세션 뷰 route.totalMeters가 null이다")
	void confirmRouteWithoutTotalMetersLeavesRouteTotalMetersNull() {
		givenLockedSession(beforeFirstConfirm());

		SessionView view = confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION));

		assertThat(savedSession().getRouteTotalMeters()).isNull();
		assertThat(view.route().totalMeters()).isNull();
	}

	@Test
	@DisplayName("두 번째 경로 확정은 세션 routeTotalMeters와 세션 뷰 route.totalMeters를 새 totalMeters로 덮어쓴다")
	void secondConfirmRouteOverwritesTotalMetersWithNewValue() {
		givenLockedSession(beforeFirstConfirm());
		confirmRouteService.confirmRoute(commandWithTotalMeters(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION, 4_200));

		SessionView view = confirmRouteService.confirmRoute(
				commandWithTotalMeters(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION + 1, 3_100));

		assertThat(savedSession().getRouteTotalMeters()).isEqualTo(3_100);
		assertThat(view.route().totalMeters()).isEqualTo(3_100);
		assertThat(view.route().routeVersion()).isEqualTo(2);
	}

	@Test
	@DisplayName("두 번째 경로 확정 요청에 totalMeters가 없으면 첫 확정의 값을 남기지 않고 세션 routeTotalMeters와 세션 뷰 route.totalMeters를 null로 덮어쓴다")
	void secondConfirmRouteWithoutTotalMetersOverwritesRouteTotalMetersWithNull() {
		givenLockedSession(beforeFirstConfirm());
		confirmRouteService.confirmRoute(commandWithTotalMeters(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION, 4_200));
		assertThat(savedSession().getRouteTotalMeters()).isEqualTo(4_200);

		SessionView view = confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION + 1));

		assertThat(savedSession().getRouteTotalMeters()).isNull();
		assertThat(view.route().totalMeters()).isNull();
		assertThat(view.route().routeVersion()).isEqualTo(2);
	}

	@Test
	@DisplayName("candidate 경로 확정은 세션의 지금 장소를 place에서 못 찾으면 422 SPOT_NOT_SAVED로 거절하고 임시 장소를 만들지 않으며 아무것도 저장 · 발행하지 않는다")
	void confirmRouteWithCandidateRejectsMissingCurrentSpotWithSpotNotSavedWithoutSavingTemporarySpot() {
		givenLockedSession(beforeFirstConfirm());
		givenTemporarySpotSaved();
		lenient().when(loadSpotPort.loadSpot(FRONT_GATE_SPOT_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION)));

		assertSpotNotSaved(exception);
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort,
				recordNotificationPort);
	}

	@Test
	@DisplayName("경로 확정은 저장한 SPOT_CONFIRMED를 append가 준 seq로 보호자와 세션 자녀에게 발행한다")
	void confirmRoutePublishesSpotConfirmedToGuardianAndChildren() {
		givenLockedSession(beforeFirstConfirm());

		confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION));

		SessionEvent appended = appendedEventOf("SPOT_CONFIRMED");
		SessionEventMessage message = publishedMessageOf("SPOT_CONFIRMED");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.stateVersion()).isEqualTo(FIRST_STATE_VERSION + 1);
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.receiverUserIds()).containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("경로 확정은 그 세션의 차량이 아니면(다른 차량) NOT_FOUND로 거절하고 아무것도 저장 · 발행하지 않는다")
	void confirmRouteRejectsOtherVehicleWithNotFound() {
		givenLockedSession(beforeFirstConfirm());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(
						command(vehicle(OTHER_VEHICLE_ID), FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("경로 확정은 요청 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절하고 아무것도 저장 · 발행하지 않는다")
	void confirmRouteRejectsStaleStateVersionWithConflictAndLatestView() {
		givenLockedSession(beforeFirstConfirm());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION - 1)));

		assertStateVersionConflictWithLatestView(exception, beforeFirstConfirm());
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("경로 확정은 spotId가 가족이 저장한 장소여도 세션의 지금 장소와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절하고 아무것도 저장 · 발행하지 않는다")
	void confirmRouteRejectsSpotDifferentFromSessionSpotWithConflictAndLatestView() {
		givenLockedSession(beforeFirstConfirm());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(command(BACK_GATE_SPOT_ID, FIRST_STATE_VERSION)));

		assertStateVersionConflictWithLatestView(exception, beforeFirstConfirm());
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("경로 확정은 끝난(COMPLETED · CANCELLED) 세션이면 SESSION_ALREADY_FINISHED로 거절하고 저장 · 알림 기록 요청 · 발행을 하지 않는다")
	void confirmRouteRejectsFinishedSessionWithSessionAlreadyFinished(SessionStatus finishedStatus) {
		givenLockedSession(finishedBeforeFirstConfirm(finishedStatus));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(command(FRONT_GATE_SPOT_ID, FIRST_STATE_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort, recordNotificationPort);
	}

	@Test
	@DisplayName("candidate 경로 확정은 세션 가족 · 세션 지금 장소의 academyId · candidate 이름 · 정차 지점=만남 지점=candidate 좌표로 임시 장소를 한 번 만든다")
	void confirmRouteWithCandidateSavesTemporarySpotOnceWithSessionFamilyAndCurrentSpotAcademy() {
		givenLockedSession(beforeFirstConfirm());
		givenTemporarySpotSaved();

		confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION));

		verify(saveTemporarySpotPort).saveTemporarySpot(FAMILY_ID, ACADEMY_ID, CANDIDATE_NAME, CANDIDATE_LOCATION,
				CANDIDATE_LOCATION);
		verifyNoMoreInteractions(saveTemporarySpotPort);
	}

	@Test
	@DisplayName("candidate 경로 확정은 세션 장소를 받은 임시 spotId로 바꾸고(spotVersion +1, 스냅샷 이름 · 좌표가 candidate 값) 경로를 확정해(CONFIRMED, routeVersion +1, totalMeters) 저장한다")
	void confirmRouteWithCandidateChangesSessionSpotToTemporarySpotAndConfirmsRoute() {
		givenLockedSession(beforeFirstConfirm());
		givenTemporarySpotSaved();

		SessionView view = confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION));

		PickupSession saved = savedSession();
		assertThat(saved.getSpotId()).isEqualTo(TEMPORARY_SPOT_ID);
		assertThat(saved.getSpotName()).isEqualTo(CANDIDATE_NAME);
		assertThat(saved.getStopPoint()).isEqualTo(CANDIDATE_LOCATION);
		assertThat(saved.getMeetingPoint()).isEqualTo(CANDIDATE_LOCATION);
		assertThat(saved.getSpotVersion()).isEqualTo(1);
		assertThat(saved.getSpotStatus()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(saved.getRouteVersion()).isEqualTo(1);
		assertThat(saved.getRouteSpotVersion()).isEqualTo(1);
		assertThat(saved.getRouteTotalMeters()).isEqualTo(CANDIDATE_TOTAL_METERS);
		assertThat(saved.getStateVersion()).isEqualTo(FIRST_STATE_VERSION + 2);
		assertThat(view.spot().spotId()).isEqualTo(TEMPORARY_SPOT_ID);
		assertThat(view.spot().name()).isEqualTo(CANDIDATE_NAME);
		assertThat(view.status().spot()).isEqualTo(SpotStatus.CONFIRMED);
		assertThat(view.route().totalMeters()).isEqualTo(CANDIDATE_TOTAL_METERS);
	}

	@Test
	@DisplayName("candidate 경로 확정은 SPOT_CHANGED 다음에 SPOT_CONFIRMED를 저장하고 둘 다 보호자와 세션 자녀에게 발행한다")
	void confirmRouteWithCandidateAppendsSpotChangedThenSpotConfirmedAndPublishesBoth() {
		givenLockedSession(beforeFirstConfirm());
		givenTemporarySpotSaved();

		confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION));

		assertThat(appendedEvents)
				.extracting(event -> event.getType().name())
				.containsSubsequence("SPOT_CHANGED", "SPOT_CONFIRMED");
		assertThat(appendedEventOf("SPOT_CONFIRMED").getStateVersion()).isEqualTo(savedSession().getStateVersion());
		assertThat(publishedMessageOf("SPOT_CHANGED").receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
		assertThat(publishedMessageOf("SPOT_CONFIRMED").receiverUserIds())
				.containsExactlyInAnyOrder(GUARDIAN_ID, MINJUN_ID, SEOYUN_ID);
	}

	@Test
	@DisplayName("candidate 첫 경로 확정도 세션 자녀에게 DEPARTURE 알림 기록을 한 번 요청하고 DEPARTURE_NOTICE_SENT를 저장한다")
	void firstConfirmRouteWithCandidateRequestsDepartureNotificationOnce() {
		givenLockedSession(beforeFirstConfirm());
		givenTemporarySpotSaved();

		confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION));

		ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(recordNotificationPort).recordNotifications(requestCaptor.capture());
		assertThat(requestCaptor.getValue().type()).isEqualTo("DEPARTURE");
		assertThat(requestCaptor.getValue().receiverUserIds()).containsExactlyInAnyOrder(MINJUN_ID, SEOYUN_ID);
		assertThat(appendedEventOf("DEPARTURE_NOTICE_SENT").getSessionId()).isEqualTo(SESSION_ID);
	}

	@Test
	@DisplayName("candidate 경로 확정은 이동 중(MEETING) 자녀가 있으면 CHILD_ALREADY_MOVING으로 거절하고 임시 장소를 만들지 않으며 아무것도 저장 · 발행하지 않는다")
	void confirmRouteWithCandidateRejectsMovingChildWithoutSavingTemporarySpot() {
		givenLockedSession(beforeFirstConfirmWith(meetingChildren()));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CHILD_ALREADY_MOVING");
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort,
				recordNotificationPort);
	}

	@Test
	@DisplayName("candidate 경로 확정은 철회되지 않은 호출이 있으면 CALL_ALREADY_SENT로 거절하고 임시 장소를 만들지 않으며 아무것도 저장 · 발행하지 않는다")
	void confirmRouteWithCandidateRejectsLiveCallWithoutSavingTemporarySpot() {
		givenLockedSession(beforeFirstConfirm());
		givenTemporarySpotSaved();
		Call liveCall = liveCall();
		lenient().when(loadCallPort.loadLiveCall(SESSION_ID)).thenReturn(Optional.of(liveCall));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(liveCall));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CALL_ALREADY_SENT");
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort,
				recordNotificationPort);
	}

	@Test
	@DisplayName("candidate 경로 확정은 요청 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절하고 임시 장소를 만들지 않는다")
	void confirmRouteWithCandidateRejectsStaleStateVersionWithoutSavingTemporarySpot() {
		givenLockedSession(beforeFirstConfirm());
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION - 1)));

		assertStateVersionConflictWithLatestView(exception, beforeFirstConfirm());
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort,
				recordNotificationPort);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("candidate 경로 확정은 끝난(COMPLETED · CANCELLED) 세션이면 SESSION_ALREADY_FINISHED로 거절하고 임시 장소를 만들지 않는다")
	void confirmRouteWithCandidateRejectsFinishedSessionWithoutSavingTemporarySpot(SessionStatus finishedStatus) {
		givenLockedSession(finishedBeforeFirstConfirm(finishedStatus));
		givenTemporarySpotSaved();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> confirmRouteService.confirmRoute(candidateCommand(FIRST_STATE_VERSION)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveTemporarySpotPort, saveSessionPort, sessionEventPort, publishSessionEventPort,
				recordNotificationPort);
	}

	// 임시 장소 포트가 TEMPORARY_SPOT_ID를 돌려주고, 그 id를 place에서 다시 읽으면 candidate 값의 임시 장소가 나온다
	private void givenTemporarySpotSaved() {
		lenient().when(saveTemporarySpotPort.saveTemporarySpot(anyLong(), anyLong(), anyString(), any(), any()))
				.thenReturn(TEMPORARY_SPOT_ID);
		lenient().when(loadSpotPort.loadSpot(TEMPORARY_SPOT_ID)).thenReturn(Optional.of(
				new SpotInfo(TEMPORARY_SPOT_ID, FAMILY_ID, ACADEMY_ID, CANDIDATE_NAME, CANDIDATE_LOCATION,
						CANDIDATE_LOCATION)));
	}

	private static ConfirmRouteCommand candidateCommand(int stateVersion) {
		return ConfirmRouteCommand.ofCandidate(vehicle(VEHICLE_ID), SESSION_ID,
				new SpotCandidate(CANDIDATE_NAME, CANDIDATE_LOCATION), NAV_ROUTE_ID, stateVersion,
				CANDIDATE_TOTAL_METERS);
	}

	private static PickupSession beforeFirstConfirmWith(List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.ROUTING,
				SpotStatus.SELECTED, FIRST_STATE_VERSION, 0, 0, null, false, NOW.plus(Duration.ofMinutes(12)), 4200,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, children);
	}

	private static List<SessionChild> meetingChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	private static Call liveCall() {
		Instant acceptedAt = NOW.minus(Duration.ofMinutes(2));
		return Call.restore(LIVE_CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN, CallStatus.SENT, 0,
				acceptedAt, acceptedAt.plusSeconds(1), null, null, null, null);
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	// 최신 세션 뷰: 요청한 차량 기준으로, 아무것도 바꾸기 전 서버 상태(stateVersion · 장소 · SELECTED 그대로)
	private static void assertStateVersionConflictWithLatestView(BusinessException exception,
			PickupSession serverSession) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("STATE_VERSION_CONFLICT");
		assertThat(exception.session()).isInstanceOfSatisfying(SessionView.class, latest -> {
			assertThat(latest.stateVersion()).isEqualTo(serverSession.getStateVersion());
			assertThat(latest.status().spot()).isEqualTo(SpotStatus.SELECTED);
			assertThat(latest).isEqualTo(ASSEMBLER.assemble(serverSession, vehicle(VEHICLE_ID),
					new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0), familyMembers(), Optional.empty(), NOW));
		});
	}

	private void givenDepartureNoticeAlreadySent() {
		lenient().when(sessionEventPort.loadFirst(SESSION_ID, "DEPARTURE_NOTICE_SENT")).thenReturn(Optional.of(
				SessionEvent.restore(EVENT_ID_BASE + 2, SESSION_ID, 2L, SessionEventType.DEPARTURE_NOTICE_SENT, 2,
						NOW.minus(Duration.ofMinutes(5)), Map.of())));
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

	private List<SessionEventMessage> publishedMessages() {
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		return captor.getAllValues();
	}

	private SessionEventMessage publishedMessageOf(String type) {
		List<SessionEventMessage> matched = publishedMessages().stream()
				.filter(message -> type.equals(message.type()))
				.toList();
		assertThat(matched).as("발행한 %s 이벤트", type).hasSize(1);
		return matched.get(0);
	}

	private static PickupSession beforeFirstConfirm() {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.ROUTING,
				SpotStatus.SELECTED, FIRST_STATE_VERSION, 0, 0, null, false, NOW.plus(Duration.ofMinutes(12)), 4200,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, waitingChildren());
	}

	// 경로를 한 번도 확정하지 못한 채 끝난(완료 · 취소) 세션
	private static PickupSession finishedBeforeFirstConfirm(SessionStatus finishedStatus) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, finishedStatus, VehiclePhase.ROUTING,
				SpotStatus.SELECTED, FIRST_STATE_VERSION, 0, 0, null, false, NOW.plus(Duration.ofMinutes(12)), 4200,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, waitingChildren());
	}

	private static PickupSession afterSpotChanged() {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, BACK_GATE_SPOT_ID, "후문 앞",
				BACK_STOP_POINT, BACK_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.ROUTING,
				SpotStatus.SELECTED, CHANGED_STATE_VERSION, 1, 1, 0, false, NOW.plus(Duration.ofMinutes(9)), 3100,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, waitingChildren());
	}

	private static List<SessionChild> waitingChildren() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	private static ConfirmRouteCommand command(long spotId, int stateVersion) {
		return command(vehicle(VEHICLE_ID), spotId, stateVersion);
	}

	private static ConfirmRouteCommand command(AuthPrincipal principal, long spotId, int stateVersion) {
		return new ConfirmRouteCommand(principal, SESSION_ID, spotId, NAV_ROUTE_ID, stateVersion);
	}

	private static ConfirmRouteCommand commandWithTotalMeters(long spotId, int stateVersion, int totalMeters) {
		return new ConfirmRouteCommand(vehicle(VEHICLE_ID), SESSION_ID, spotId, NAV_ROUTE_ID, stateVersion,
				totalMeters);
	}

	private static AuthPrincipal vehicle(long vehicleId) {
		return new AuthPrincipal(PrincipalType.VEHICLE, vehicleId, FAMILY_ID, null);
	}

	private static List<MemberInfo> familyMembers() {
		return List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null));
	}

	private static void assertSpotNotSaved(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("SPOT_NOT_SAVED");
	}

}

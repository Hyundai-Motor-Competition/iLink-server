package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.MarkChildReadyCommand;
import com.example.pickupservice.pickup.application.port.in.MarkChildReadyResult;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
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

// SIG-06 준비 완료: 자녀 주체가 누른다. 화면 표시용이라 childPhase · 호출 판정은 바꾸지 않고 readyAt만 기록한다
@ExtendWith(MockitoExtension.class)
class MarkChildReadyServiceTest {

	// 한국 시간 2026-11-13(금) 18:09
	private static final Instant NOW = Instant.parse("2026-11-13T09:09:00Z");
	private static final Instant FIRST_READY_AT = NOW.minusSeconds(40);
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 13);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long SIBLING_NOT_IN_SESSION_ID = 36L;
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
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SessionEventPort sessionEventPort;

	@Mock
	private PublishSessionEventPort publishSessionEventPort;

	private MarkChildReadyService markChildReadyService;

	private final AtomicLong nextSeq = new AtomicLong(LAST_SEQ);
	private final List<SessionEvent> appendedEvents = new ArrayList<>();

	@BeforeEach
	void setUp() {
		markChildReadyService = new MarkChildReadyService(loadSessionPort, saveSessionPort, loadFamilyPort,
				sessionEventPort, publishSessionEventPort, Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
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
	@DisplayName("준비 완료는 세션 행을 잠그고 그 자녀의 readyAt에 지금 시각을 기록해 저장하며 childUserId · readyAt을 돌려준다 — childPhase와 형제는 그대로")
	void markReadyRecordsReadyAtOfThatChildOnlyAndReturnsIt() {
		PickupSession session = calledSession(ACTIVE, false);
		givenLockedSession(session);

		MarkChildReadyResult result = markChildReadyService.markReady(command(MINJUN_ID));

		assertThat(result).isEqualTo(new MarkChildReadyResult(MINJUN_ID, NOW));
		verify(saveSessionPort, atLeastOnce()).save(session);
		assertThat(session.getChildren())
				.extracting(SessionChild::getChildUserId, SessionChild::getChildPhase, SessionChild::getReadyAt)
				.containsExactlyInAnyOrder(
						tuple(MINJUN_ID, ChildPhase.CALLED, NOW),
						tuple(SEOYUN_ID, ChildPhase.CALLED, null));
	}

	@Test
	@DisplayName("준비 완료는 세션을 저장한 뒤 CHILD_READY를 payload childUserId만 담아 저장한 세션의 stateVersion으로 한 번 저장한다")
	void markReadyAppendsChildReadyWithChildUserIdAfterSavingSession() {
		PickupSession session = calledSession(ACTIVE, false);
		givenLockedSession(session);

		markChildReadyService.markReady(command(MINJUN_ID));

		InOrder inOrder = inOrder(saveSessionPort, sessionEventPort);
		inOrder.verify(saveSessionPort, atLeastOnce()).save(any(PickupSession.class));
		inOrder.verify(sessionEventPort).append(eq(SESSION_ID), eq("CHILD_READY"), eq(session.getStateVersion()),
				eq(NOW), any());
		assertThat(appendedEvents).hasSize(1);
		assertThat(appendedEvents.get(0).getType()).isEqualTo(SessionEventType.CHILD_READY);
		assertThat(appendedEvents.get(0).getPayload()).containsOnly(entry("childUserId", MINJUN_ID));
	}

	@Test
	@DisplayName("준비 완료는 저장한 CHILD_READY를 append가 준 seq로 보호자에게 발행한다(차량은 SSE 리스너가 세션으로 보낸다)")
	void markReadyPublishesChildReadyToGuardians() {
		givenLockedSession(calledSession(ACTIVE, false));

		markChildReadyService.markReady(command(MINJUN_ID));

		SessionEvent appended = appendedEvents.get(0);
		SessionEventMessage message = publishedMessage();
		assertThat(message.type()).isEqualTo("CHILD_READY");
		assertThat(message.sessionId()).isEqualTo(SESSION_ID);
		assertThat(message.eventId()).isEqualTo(appended.getEventId());
		assertThat(message.seq()).isEqualTo(appended.getSeq());
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.payload()).containsEntry("childUserId", MINJUN_ID);
		assertThat(message.receiverUserIds()).containsExactly(GUARDIAN_ID);
	}

	@Test
	@DisplayName("준비 완료를 이미 누른 자녀가 다시 누르면 첫 readyAt을 돌려주고 CHILD_READY를 다시 저장 · 발행하지 않는다")
	void markReadyAgainReturnsFirstReadyAtWithoutNewEvent() {
		PickupSession session = calledSession(ACTIVE, true);
		givenLockedSession(session);

		MarkChildReadyResult result = markChildReadyService.markReady(command(MINJUN_ID));

		assertThat(result).isEqualTo(new MarkChildReadyResult(MINJUN_ID, FIRST_READY_AT));
		assertThat(session.getChildren())
				.filteredOn(child -> child.getChildUserId() == MINJUN_ID)
				.extracting(SessionChild::getReadyAt)
				.containsExactly(FIRST_READY_AT);
		verify(sessionEventPort, never()).append(anyLong(), anyString(), anyInt(), any(), any());
		verifyNoInteractions(publishSessionEventPort);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("준비 완료는 끝난(COMPLETED · CANCELLED) 세션이면 409 SESSION_ALREADY_FINISHED로 거절하고 저장 · 발행을 하지 않는다")
	void markReadyRejectsFinishedSessionWithSessionAlreadyFinished(SessionStatus finishedStatus) {
		givenLockedSession(calledSession(finishedStatus, false));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> markChildReadyService.markReady(command(MINJUN_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("준비 완료는 세션 자녀가 아닌 같은 가족 자녀면 404 NOT_FOUND로 거절하고 저장 · 발행을 하지 않는다")
	void markReadyRejectsChildNotInSessionWithNotFound() {
		givenLockedSession(calledSession(ACTIVE, false));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> markChildReadyService.markReady(command(SIBLING_NOT_IN_SESSION_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	@Test
	@DisplayName("준비 완료는 세션이 없으면 404 NOT_FOUND로 거절한다")
	void markReadyRejectsUnknownSessionWithNotFound() {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> markChildReadyService.markReady(command(MINJUN_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(saveSessionPort, sessionEventPort, publishSessionEventPort);
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private SessionEventMessage publishedMessage() {
		ArgumentCaptor<SessionEventMessage> captor = ArgumentCaptor.forClass(SessionEventMessage.class);
		verify(publishSessionEventPort, atLeastOnce()).publish(captor.capture());
		assertThat(captor.getAllValues()).hasSize(1);
		return captor.getValue();
	}

	// 합류 안내를 받고 두 자녀가 차로 오는 중인 세션. minjunAlreadyReady면 민준이 40초 전에 이미 준비 완료를 눌렀다
	private static PickupSession calledSession(SessionStatus status, boolean minjunAlreadyReady) {
		SessionChild minjun = SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
				null, null, null, null);
		if (minjunAlreadyReady) {
			minjun.markReady(FIRST_READY_AT);
		}
		SessionChild seoyun = SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
				null, null, null, null);
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, NOW.minusSeconds(20), 0,
				NOW.minusSeconds(5), null, null, null, null, null, null, null, null, List.of(minjun, seoyun));
	}

	private static final SessionStatus ACTIVE = SessionStatus.ACTIVE;

	private static MarkChildReadyCommand command(long childUserId) {
		AuthPrincipal child = new AuthPrincipal(PrincipalType.USER, childUserId, FAMILY_ID, Role.CHILD);
		return new MarkChildReadyCommand(child, SESSION_ID);
	}

}

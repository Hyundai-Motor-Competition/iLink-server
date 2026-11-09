package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.HoldCommand;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.IncidentType;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HoldSessionServiceTest {

	// 한국 시간 2026-11-09(월) 17:50
	private static final Instant NOW = Instant.parse("2026-11-09T08:50:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long DELEGATE_ID = 35L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final int STATE_VERSION = 8;
	private static final String HOLD_REASON = "아이가 화장실에 가서 잠깐 멈춥니다";

	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_GUARDIAN_ID = 91L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final Instant INCIDENT_OPENED_AT = NOW.minus(Duration.ofMinutes(3));

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadCallPort loadCallPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	private HoldSessionService holdSessionService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, clock);
		holdSessionService = new HoldSessionService(loadSessionPort, saveSessionPort, loadCallPort, loadFamilyPort,
				sessionAccess, clock);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(family());
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(familyMembers());
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.empty());
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("세션 보류는 잠가 읽은 ACTIVE 세션을 ON_HOLD · stateVersion +1로 저장한다")
	void holdSavesActiveSessionAsOnHold() {
		givenLockedSession(session(SessionStatus.ACTIVE));

		holdSessionService.hold(new HoldCommand(guardian(), SESSION_ID, STATE_VERSION, HOLD_REASON));

		verify(loadSessionPort).loadForUpdate(SESSION_ID);
		PickupSession saved = savedSession();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.ON_HOLD);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
	}

	@Test
	@DisplayName("세션 보류는 저장한 세션으로 요청 주체 기준 세션 뷰(status.session ON_HOLD · 올린 stateVersion)를 돌려준다")
	void holdReturnsViewOfSavedSessionForPrincipal() {
		givenLockedSession(session(SessionStatus.ACTIVE));

		SessionView view = holdSessionService.hold(
				new HoldCommand(delegate(), SESSION_ID, STATE_VERSION, HOLD_REASON));

		PickupSession saved = savedSession();
		assertThat(view.sessionId()).isEqualTo(SESSION_ID);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.status().session()).isEqualTo(SessionStatus.ON_HOLD);
		assertThat(view).isEqualTo(SessionViewAssembler.assemble(saved, delegate(), family(), familyMembers(),
				Optional.empty(), NOW));
	}

	@Test
	@DisplayName("보류 해제는 잠가 읽은 ON_HOLD 세션을 ACTIVE · stateVersion +1로 저장하고 status.session ACTIVE인 세션 뷰를 돌려준다")
	void releaseHoldSavesOnHoldSessionAsActiveAndReturnsView() {
		givenLockedSession(session(SessionStatus.ON_HOLD));

		SessionView view = holdSessionService.releaseHold(
				new HoldCommand(vehicle(), SESSION_ID, STATE_VERSION, null));

		verify(loadSessionPort).loadForUpdate(SESSION_ID);
		PickupSession saved = savedSession();
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.stateVersion()).isEqualTo(STATE_VERSION + 1);
		assertThat(view.status().session()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(view).isEqualTo(SessionViewAssembler.assemble(saved, vehicle(), family(), familyMembers(),
				Optional.empty(), NOW));
	}

	@Test
	@DisplayName("보류 해제는 열린 예외가 있으면 incidentResolvedAt을 지금 시각으로 채워 저장한다")
	void releaseHoldResolvesOpenIncidentAtNow() {
		givenLockedSession(onHoldWithOpenIncident());

		holdSessionService.releaseHold(new HoldCommand(vehicle(), SESSION_ID, STATE_VERSION, null));

		PickupSession saved = savedSession();
		assertThat(saved.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);
		assertThat(saved.getIncidentOpenedAt()).isEqualTo(INCIDENT_OPENED_AT);
		assertThat(saved.getIncidentResolvedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("세션 보류는 요청 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절하고 저장하지 않는다")
	void holdRejectsStaleStateVersionWithConflictAndLatestView() {
		givenLockedSession(session(SessionStatus.ACTIVE));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> holdSessionService.hold(
						new HoldCommand(guardian(), SESSION_ID, STATE_VERSION - 1, HOLD_REASON)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("STATE_VERSION_CONFLICT");
		assertThat(exception.session()).isInstanceOfSatisfying(SessionView.class, latest -> {
			assertThat(latest.stateVersion()).isEqualTo(STATE_VERSION);
			assertThat(latest.status().session()).isEqualTo(SessionStatus.ACTIVE);
		});
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("보류 해제는 요청 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT로 거절하고 저장하지 않는다")
	void releaseHoldRejectsStaleStateVersionWithConflict() {
		givenLockedSession(session(SessionStatus.ON_HOLD));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> holdSessionService.releaseHold(
						new HoldCommand(guardian(), SESSION_ID, STATE_VERSION + 1, null)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("STATE_VERSION_CONFLICT");
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("세션 보류는 세션을 볼 수 없는 다른 가족 주체면 NOT_FOUND로 거절하고 저장하지 않는다")
	void holdRejectsPrincipalOutsideSessionWithNotFound() {
		givenLockedSession(session(SessionStatus.ACTIVE));
		AuthPrincipal otherGuardian = new AuthPrincipal(PrincipalType.USER, OTHER_FAMILY_GUARDIAN_ID,
				OTHER_FAMILY_ID, Role.GUARDIAN);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> holdSessionService.hold(new HoldCommand(otherGuardian, SESSION_ID, STATE_VERSION, HOLD_REASON)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("보류 해제는 세션이 없으면 NOT_FOUND로 거절하고 저장하지 않는다")
	void releaseHoldRejectsMissingSessionWithNotFound() {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> holdSessionService.releaseHold(new HoldCommand(guardian(), SESSION_ID, STATE_VERSION, null)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("세션 보류는 취소된 세션이면 409 SESSION_ALREADY_FINISHED로 거절하고 저장하지 않는다")
	void holdRejectsCancelledSessionWithSessionAlreadyFinished() {
		givenLockedSession(session(SessionStatus.CANCELLED));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> holdSessionService.hold(new HoldCommand(guardian(), SESSION_ID, STATE_VERSION, HOLD_REASON)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("보류 해제는 완료된 세션이면 409 SESSION_ALREADY_FINISHED로 거절하고 저장하지 않는다")
	void releaseHoldRejectsCompletedSessionWithSessionAlreadyFinished() {
		givenLockedSession(session(SessionStatus.COMPLETED));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> holdSessionService.releaseHold(new HoldCommand(vehicle(), SESSION_ID, STATE_VERSION, null)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort);
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort, atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}

	private static FamilyInfo family() {
		return new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0);
	}

	private static List<MemberInfo> familyMembers() {
		return List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null));
	}

	private static AuthPrincipal guardian() {
		return new AuthPrincipal(PrincipalType.USER, GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
	}

	private static AuthPrincipal delegate() {
		return new AuthPrincipal(PrincipalType.USER, DELEGATE_ID, FAMILY_ID, Role.DELEGATE);
	}

	private static AuthPrincipal vehicle() {
		return new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	}

	private static PickupSession session(SessionStatus status) {
		return session(status, null, null);
	}

	// 차량 탑승 확인 중 자녀가 없어 예외(CHILD_NOT_ON_BOARD)를 열어 ON_HOLD가 된 세션
	private static PickupSession onHoldWithOpenIncident() {
		return session(SessionStatus.ON_HOLD, IncidentType.CHILD_NOT_ON_BOARD, INCIDENT_OPENED_AT);
	}

	private static PickupSession session(SessionStatus status, IncidentType incidentType, Instant incidentOpenedAt) {
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, NOW.plus(Duration.ofMinutes(6)), 2400,
				NOW.minusSeconds(10), null, null, null, incidentType, null, incidentOpenedAt, null, children);
	}

}

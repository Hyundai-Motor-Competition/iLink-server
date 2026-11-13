package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ChildLocationCommand;
import com.example.pickupservice.pickup.application.port.out.LoadChildReadinessPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// SIG-04 자녀 위치: 위치 이벤트는 19종에 없어 저장 · 발행하지 않으므로 서비스는 이벤트 포트를 받지 않는다
@ExtendWith(MockitoExtension.class)
class ReportChildLocationServiceTest {

	// 한국 시간 2026-11-09(월) 17:58
	private static final Instant NOW = Instant.parse("2026-11-09T08:58:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final int STATE_VERSION = 9;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);

	// 민준이 15초 전에 보낸 위치, 서윤이 10초 전에 보낸 위치
	private static final LatLng MINJUN_OLD_LOCATION = new LatLng(37.501500, 127.039700);
	private static final Instant MINJUN_OLD_LOCATED_AT = NOW.minusSeconds(15);
	private static final LatLng SEOYUN_LOCATION = new LatLng(37.501550, 127.039650);
	private static final Instant SEOYUN_LOCATED_AT = NOW.minusSeconds(10);

	// 이번에 민준이 보내는 위치
	private static final double REPORTED_LAT = 37.501620;
	private static final double REPORTED_LNG = 127.039580;
	private static final Instant REPORTED_AT = NOW.minusSeconds(1);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private SaveSessionPort saveSessionPort;

	@Mock
	private LoadChildReadinessPort loadChildReadinessPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	private ReportChildLocationService reportChildLocationService;

	@BeforeEach
	void setUp() {
		reportChildLocationService = new ReportChildLocationService(loadSessionPort, saveSessionPort,
				loadChildReadinessPort, loadFamilyPort);
		lenient().when(loadChildReadinessPort.hasLocationConsent(anyLong())).thenReturn(true);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(familyInfo(true));
		lenient().when(saveSessionPort.save(any(PickupSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("자녀 위치는 위치 동의가 없으면 LOCATION_CONSENT_REQUIRED로 거절한다 — 보낸 자녀의 동의를 보고 403을 주며 세션을 저장하지 않는다")
	void reportRejectsWithoutLocationConsent() {
		givenLockedSession(calledSession(ChildPhase.CALLED));
		when(loadChildReadinessPort.hasLocationConsent(MINJUN_ID)).thenReturn(false);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> reportChildLocationService.report(minjunCommand()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(exception.code()).isEqualTo("LOCATION_CONSENT_REQUIRED");
		verify(loadChildReadinessPort).hasLocationConsent(MINJUN_ID);
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("자녀 위치는 가족의 자녀 위치 사용이 꺼져 있으면 403 LOCATION_CONSENT_REQUIRED로 거절한다 — 자녀 동의가 true여도 거절하고 세션을 저장하지 않는다")
	void reportRejectsWhenFamilyChildLocationDisabled() {
		givenLockedSession(calledSession(ChildPhase.CALLED));
		lenient().when(loadChildReadinessPort.hasLocationConsent(MINJUN_ID)).thenReturn(true);
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(familyInfo(false));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> reportChildLocationService.report(minjunCommand()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(exception.code()).isEqualTo("LOCATION_CONSENT_REQUIRED");
		verify(loadFamilyPort).loadFamily(FAMILY_ID);
		verifyNoInteractions(saveSessionPort);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("자녀 위치는 끝난 세션이면 SESSION_ALREADY_FINISHED로 거절한다 — 자녀가 CALLED로 남아 있어도 409를 주며 세션을 저장하지 않는다")
	void reportRejectsFinishedSessionWithSessionAlreadyFinished(SessionStatus finished) {
		givenLockedSession(finishedSession(finished));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> reportChildLocationService.report(minjunCommand()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SESSION_ALREADY_FINISHED");
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("자녀 위치는 WAITING 자녀면 409로 거절한다 — 코드 CHILD_NOT_CALLED, 세션을 저장하지 않는다")
	void reportRejectsWaitingChildWithChildNotCalled() {
		givenLockedSession(waitingSession());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> reportChildLocationService.report(minjunCommand()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("CHILD_NOT_CALLED");
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("자녀 위치는 CALLED 자녀의 lastLat · lastLng · lastLocatedAt을 덮어쓴다 — 형제 위치 · childPhase · stateVersion은 그대로 저장한다")
	void reportOverwritesCalledChildLocation() {
		givenLockedSession(calledSession(ChildPhase.CALLED));

		reportChildLocationService.report(minjunCommand());

		assertLocationOverwritten(savedSession(), ChildPhase.CALLED);
	}

	@Test
	@DisplayName("자녀 위치는 호출을 확인한 MEETING 자녀의 위치도 덮어쓴다(설계서 SIG-04 「CALLED · MEETING만 받는다」)")
	void reportOverwritesMeetingChildLocation() {
		givenLockedSession(calledSession(ChildPhase.MEETING));

		reportChildLocationService.report(minjunCommand());

		assertLocationOverwritten(savedSession(), ChildPhase.MEETING);
	}

	private static void assertLocationOverwritten(PickupSession saved, ChildPhase minjunPhase) {
		SessionChild minjun = childOf(saved, MINJUN_ID);
		assertThat(minjun.getLastLocation()).as("민준 lastLat · lastLng")
				.isEqualTo(new LatLng(REPORTED_LAT, REPORTED_LNG));
		assertThat(minjun.getLastLocatedAt()).as("민준 lastLocatedAt").isEqualTo(REPORTED_AT);
		assertThat(minjun.getChildPhase()).as("민준 childPhase").isEqualTo(minjunPhase);
		SessionChild seoyun = childOf(saved, SEOYUN_ID);
		assertThat(seoyun.getLastLocation()).as("서윤 lastLat · lastLng").isEqualTo(SEOYUN_LOCATION);
		assertThat(seoyun.getLastLocatedAt()).as("서윤 lastLocatedAt").isEqualTo(SEOYUN_LOCATED_AT);
		assertThat(saved.getStateVersion()).as("stateVersion").isEqualTo(STATE_VERSION);
	}

	// 세션을 바꾸는 요청이므로 행 잠금(loadForUpdate)으로 읽는다(설계서 「세션 행 잠금 범위」)
	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort).save(captor.capture());
		return captor.getValue();
	}

	private static SessionChild childOf(PickupSession session, long childUserId) {
		return session.getChildren().stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.findFirst()
				.orElseThrow();
	}

	// 합류 안내를 받아 두 자녀가 위치를 보내는 중인 세션. MEETING이면 민준이 호출을 확인한 상태
	private static PickupSession calledSession(ChildPhase minjunPhase) {
		Instant minjunAcknowledgedAt = minjunPhase == ChildPhase.MEETING ? NOW.minusSeconds(30) : null;
		return session(SessionStatus.ACTIVE, List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, minjunPhase, 0, null,
						MINJUN_OLD_LOCATION, MINJUN_OLD_LOCATED_AT, minjunAcknowledgedAt, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						SEOYUN_LOCATION, SEOYUN_LOCATED_AT, null, null)));
	}

	// 호출 전(또는 철회 뒤) 두 자녀가 모두 WAITING이고 위치가 없는 세션
	private static PickupSession waitingSession() {
		return session(SessionStatus.ACTIVE, List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null)));
	}

	// 끝나면서 위치는 비웠지만 childPhase는 CALLED로 남은 세션
	private static PickupSession finishedSession(SessionStatus finished) {
		return session(finished, List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null)));
	}

	private static PickupSession session(SessionStatus status, List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, STATE_VERSION, 0, 1, 0, false, NOW.plus(Duration.ofMinutes(2)), 600,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, children);
	}

	// 가족의 자녀 위치 사용(childLocationEnabled) 설정을 담은 FamilyInfo
	private static FamilyInfo familyInfo(boolean childLocationEnabled) {
		return new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0, childLocationEnabled);
	}

	private static ChildLocationCommand minjunCommand() {
		AuthPrincipal minjun = new AuthPrincipal(PrincipalType.USER, MINJUN_ID, FAMILY_ID, Role.CHILD);
		return new ChildLocationCommand(minjun, SESSION_ID, REPORTED_AT, REPORTED_LAT, REPORTED_LNG, 8.0);
	}

}

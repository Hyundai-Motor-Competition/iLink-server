package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.RejectBoardingCommand;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.ChildPhase;
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
class RejectBoardingServiceTest {

	// 한국 시간 2026-11-09(월) 18:11:55. 3초 전 문이 열려 탑승 인식, 취소 창은 7초 남음
	private static final Instant NOW = Instant.parse("2026-11-09T09:11:55Z");
	private static final Instant BOARDING_DETECTED_AT = NOW.minusSeconds(3);
	private static final Instant BOARDING_CANCELABLE_UNTIL = BOARDING_DETECTED_AT.plusSeconds(10);
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long SPOT_ID = 21L;
	private static final int STATE_VERSION = 12;

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

	private RejectBoardingService rejectBoardingService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SessionAccess sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, clock);
		rejectBoardingService = new RejectBoardingService(loadSessionPort, saveSessionPort, loadFamilyPort,
				loadCallPort, sessionAccess, clock);
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
	}

	@Test
	@DisplayName("탑승 취소는 자녀가 여러 명인데 childUserId가 없으면 fields에 childUserId를 담은 VALIDATION_FAILED로 거절하고 세션을 저장하지 않는다")
	void rejectBoardingWithoutChildUserIdForSiblingsFailsWithValidationFailed() {
		givenLockedSession(boardedSession(List.of(boardedChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID),
				boardedChild(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID))));

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> rejectBoardingService.rejectBoarding(command(null)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
		assertThat(exception.fields())
				.extracting(FieldError::field)
				.containsExactly("childUserId");
		verifyNoInteractions(saveSessionPort);
	}

	@Test
	@DisplayName("탑승 취소는 자녀가 1명이면 childUserId가 없어도 그 자녀의 boardedAt을 비우고 stateVersion을 1 올려 저장한다")
	void rejectBoardingWithoutChildUserIdForSingleChildClearsThatChild() {
		givenLockedSession(boardedSession(List.of(boardedChild(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID))));

		rejectBoardingService.rejectBoarding(command(null));

		PickupSession saved = savedSession();
		assertThat(saved.getChildren()).hasSize(1);
		assertThat(saved.boardedAtOf(MINJUN_ID)).as("민준 boardedAt").isNull();
		assertThat(saved.getStateVersion()).isEqualTo(STATE_VERSION + 1);
	}

	private void givenLockedSession(PickupSession session) {
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private PickupSession savedSession() {
		ArgumentCaptor<PickupSession> captor = ArgumentCaptor.forClass(PickupSession.class);
		verify(saveSessionPort, atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}

	private static SessionChild boardedChild(long sessionChildId, long childUserId, long scheduleId) {
		return SessionChild.restore(sessionChildId, childUserId, scheduleId, ChildPhase.MEETING, 0, null, null, null,
				NOW.minusSeconds(60), BOARDING_DETECTED_AT);
	}

	// 정차 중 문 1회로 전원 탑승 인식, 취소 창 안
	private static PickupSession boardedSession(List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, "해솔영어학원 정문 앞", STOP_POINT,
				MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.STOPPED, SpotStatus.CONFIRMED,
				STATE_VERSION, 0, 1, 0, false, NOW.minusSeconds(10), 0, NOW.minusSeconds(5), BOARDING_DETECTED_AT,
				BOARDING_CANCELABLE_UNTIL, BoardingDoor.REAR_RIGHT, null, null, null, null, children);
	}

	private static RejectBoardingCommand command(Long childUserId) {
		AuthPrincipal vehicle = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
		return new RejectBoardingCommand(vehicle, SESSION_ID, childUserId, STATE_VERSION);
	}

}

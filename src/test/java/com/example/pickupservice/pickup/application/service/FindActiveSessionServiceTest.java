package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ActiveSessionResult;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FindActiveSessionServiceTest {

	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_ID = 31L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;

	private static final long VEHICLE_SESSION_ID = 501L;
	private static final long FAMILY_SESSION_ID = 502L;
	private static final long CHILD_SESSION_ID = 503L;

	@Mock
	private LoadSessionPort loadSessionPort;

	private FindActiveSessionService findActiveSessionService;

	@BeforeEach
	void setUp() {
		findActiveSessionService = new FindActiveSessionService(loadSessionPort);
		lenient().when(loadSessionPort.loadActiveByVehicle(anyLong())).thenReturn(Optional.empty());
		lenient().when(loadSessionPort.loadActiveByFamily(anyLong())).thenReturn(Optional.empty());
		lenient().when(loadSessionPort.loadActiveByChild(anyLong())).thenReturn(Optional.empty());
	}

	@Test
	@DisplayName("진행 중 세션 찾기는 차량 주체에 그 차량의 진행 중 세션을 돌려준다")
	void findActiveReturnsSessionOfThatVehicleForVehiclePrincipal() {
		givenEachLookupFindsDifferentSession();

		Optional<ActiveSessionResult> result = findActiveSessionService.findActive(vehicle());

		assertThat(result).contains(new ActiveSessionResult(VEHICLE_SESSION_ID, SessionStatus.ACTIVE, 3));
	}

	@Test
	@DisplayName("진행 중 세션 찾기는 자녀 주체에 자기가 포함된 진행 중 세션을 돌려준다")
	void findActiveReturnsSessionIncludingThatChildForChildPrincipal() {
		givenEachLookupFindsDifferentSession();

		Optional<ActiveSessionResult> result = findActiveSessionService.findActive(user(MINJUN_ID, Role.CHILD));

		assertThat(result).contains(new ActiveSessionResult(CHILD_SESSION_ID, SessionStatus.ON_HOLD, 5));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = Role.class, names = {"GUARDIAN", "CO_GUARDIAN", "DELEGATE", "TEACHER"})
	@DisplayName("진행 중 세션 찾기는 자녀가 아닌 사용자 주체에 자기 가족의 진행 중 세션을 돌려준다")
	void findActiveReturnsSessionOfOwnFamilyForNonChildUserPrincipal(Role role) {
		givenEachLookupFindsDifferentSession();

		Optional<ActiveSessionResult> result = findActiveSessionService.findActive(user(GUARDIAN_ID, role));

		assertThat(result).contains(new ActiveSessionResult(FAMILY_SESSION_ID, SessionStatus.ACTIVE, 4));
	}

	@Test
	@DisplayName("진행 중 세션 찾기는 차량 주체에 그 차량의 진행 중 세션이 없으면 빈 값을 돌려준다")
	void findActiveReturnsEmptyForVehiclePrincipalWithoutActiveSession() {
		Optional<ActiveSessionResult> result = findActiveSessionService.findActive(vehicle());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("진행 중 세션 찾기는 자녀 주체에 가족 세션이 있어도 자기가 포함된 세션이 없으면 빈 값을 돌려준다")
	void findActiveReturnsEmptyForChildPrincipalNotIncludedInFamilySession() {
		// 가족 세션에는 서윤만 있고 민준은 없다
		lenient().when(loadSessionPort.loadActiveByFamily(FAMILY_ID)).thenReturn(Optional.of(
				session(FAMILY_SESSION_ID, SessionStatus.ACTIVE, 4, child(SEOYUN_ID, SEOYUN_SCHEDULE_ID))));

		Optional<ActiveSessionResult> result = findActiveSessionService.findActive(user(MINJUN_ID, Role.CHILD));

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("진행 중 세션 찾기는 보호자 주체에 자기 가족의 진행 중 세션이 없으면 빈 값을 돌려준다")
	void findActiveReturnsEmptyForGuardianPrincipalWithoutFamilySession() {
		Optional<ActiveSessionResult> result = findActiveSessionService.findActive(user(GUARDIAN_ID, Role.GUARDIAN));

		assertThat(result).isEmpty();
	}

	// 조회 방법마다 다른 세션이 나오게 해서, 주체에 맞는 조회를 썼는지 sessionId로 가린다
	private void givenEachLookupFindsDifferentSession() {
		lenient().when(loadSessionPort.loadActiveByVehicle(VEHICLE_ID)).thenReturn(Optional.of(
				session(VEHICLE_SESSION_ID, SessionStatus.ACTIVE, 3,
						child(MINJUN_ID, MINJUN_SCHEDULE_ID), child(SEOYUN_ID, SEOYUN_SCHEDULE_ID))));
		lenient().when(loadSessionPort.loadActiveByFamily(FAMILY_ID)).thenReturn(Optional.of(
				session(FAMILY_SESSION_ID, SessionStatus.ACTIVE, 4,
						child(MINJUN_ID, MINJUN_SCHEDULE_ID), child(SEOYUN_ID, SEOYUN_SCHEDULE_ID))));
		lenient().when(loadSessionPort.loadActiveByChild(MINJUN_ID)).thenReturn(Optional.of(
				session(CHILD_SESSION_ID, SessionStatus.ON_HOLD, 5, child(MINJUN_ID, MINJUN_SCHEDULE_ID))));
	}

	private static AuthPrincipal vehicle() {
		return new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	}

	private static AuthPrincipal user(long userId, Role role) {
		return new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
	}

	private static PickupSession session(long sessionId, SessionStatus status, int stateVersion,
			SessionChild... children) {
		return PickupSession.restore(sessionId, VEHICLE_ID, FAMILY_ID, 21L, "정문 앞",
				new LatLng(37.501400, 127.039950), new LatLng(37.501301, 127.039901), TODAY_KST, status,
				VehiclePhase.ROUTING, SpotStatus.SELECTED, stateVersion, 0, 0, null, false, null, null, null, null,
				null, null, null, null, null, null, null, List.of(children));
	}

	private static SessionChild child(long childUserId, long scheduleId) {
		return SessionChild.restore(null, childUserId, scheduleId, ChildPhase.WAITING, 0, null, null, null, null,
				null);
	}

}

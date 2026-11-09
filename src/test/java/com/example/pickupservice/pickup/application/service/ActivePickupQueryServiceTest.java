package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivePickupQueryServiceTest {

	// 한국 시간 2026-11-09(월) 17:50
	private static final Instant NOW = Instant.parse("2026-11-09T08:50:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long FRONT_GATE_SPOT_ID = 21L;
	private static final long TEACHER_ID = 40L;

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng MINJUN_LOCATION = new LatLng(37.501500, 127.039700);

	@Mock
	private LoadSessionPort loadSessionPort;

	private ActivePickupQueryService activePickupQueryService;

	@BeforeEach
	void setUp() {
		activePickupQueryService = new ActivePickupQueryService(loadSessionPort);
	}

	@ParameterizedTest(name = "sessionStatus {0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("진행 중 픽업 확인 hasActivePickup은 그 차량의 진행 중(ACTIVE · ON_HOLD) 세션이 있으면 true를 돌려준다")
	void hasActivePickupIsTrueWhenVehicleHasActiveOrOnHoldSession(SessionStatus status) {
		when(loadSessionPort.loadActiveByVehicle(VEHICLE_ID)).thenReturn(Optional.of(session(status, noLocation())));

		assertThat(activePickupQueryService.hasActivePickup(VEHICLE_ID)).isTrue();
	}

	@Test
	@DisplayName("진행 중 픽업 확인 hasActivePickup은 그 차량의 진행 중 세션이 없으면 false를 돌려준다")
	void hasActivePickupIsFalseWhenVehicleHasNoActiveSession() {
		when(loadSessionPort.loadActiveByVehicle(VEHICLE_ID)).thenReturn(Optional.empty());

		assertThat(activePickupQueryService.hasActivePickup(VEHICLE_ID)).isFalse();
	}

	@ParameterizedTest(name = "sessionStatus {0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("진행 중 픽업 확인 isInActivePickup은 그 사용자가 자녀로 들어 있는 진행 중(ACTIVE · ON_HOLD) 세션이 있으면 true를 돌려준다")
	void isInActivePickupIsTrueWhenUserIsChildOfActiveOrOnHoldSession(SessionStatus status) {
		when(loadSessionPort.loadActiveByChild(MINJUN_ID)).thenReturn(Optional.of(session(status, noLocation())));

		assertThat(activePickupQueryService.isInActivePickup(MINJUN_ID)).isTrue();
	}

	@Test
	@DisplayName("진행 중 픽업 확인 isInActivePickup은 그 사용자가 자녀로 들어 있는 진행 중 세션이 없으면 false를 돌려준다")
	void isInActivePickupIsFalseWhenUserIsInNoActiveSession() {
		when(loadSessionPort.loadActiveByChild(TEACHER_ID)).thenReturn(Optional.empty());

		assertThat(activePickupQueryService.isInActivePickup(TEACHER_ID)).isFalse();
	}

	@ParameterizedTest(name = "sessionStatus {0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("진행 중 픽업 확인 isSpotInUse는 그 장소를 쓰는 진행 중(ACTIVE · ON_HOLD) 세션이 있으면 true를 돌려준다")
	void isSpotInUseIsTrueWhenActiveOrOnHoldSessionUsesSpot(SessionStatus status) {
		when(loadSessionPort.loadActiveBySpot(FRONT_GATE_SPOT_ID))
				.thenReturn(Optional.of(session(status, noLocation())));

		assertThat(activePickupQueryService.isSpotInUse(FRONT_GATE_SPOT_ID)).isTrue();
	}

	@Test
	@DisplayName("진행 중 픽업 확인 isSpotInUse는 그 장소를 쓰는 진행 중 세션이 없으면 false를 돌려준다")
	void isSpotInUseIsFalseWhenNoActiveSessionUsesSpot() {
		when(loadSessionPort.loadActiveBySpot(FRONT_GATE_SPOT_ID)).thenReturn(Optional.empty());

		assertThat(activePickupQueryService.isSpotInUse(FRONT_GATE_SPOT_ID)).isFalse();
	}

	@ParameterizedTest(name = "sessionStatus {0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("진행 중 픽업 확인 activeChildLocation은 가족의 진행 중(ACTIVE · ON_HOLD) 세션에서 위치를 보낸 자녀의 마지막 위치를 돌려준다")
	void activeChildLocationReturnsLastLocationOfChildInActiveSession(SessionStatus status) {
		when(loadSessionPort.loadActiveByFamily(FAMILY_ID))
				.thenReturn(Optional.of(session(status, minjunLocatedSeoyunNot())));

		assertThat(activePickupQueryService.activeChildLocation(FAMILY_ID)).contains(MINJUN_LOCATION);
	}

	@Test
	@DisplayName("진행 중 픽업 확인 activeChildLocation은 가족의 진행 중 세션이 없으면 빈 값을 돌려준다")
	void activeChildLocationIsEmptyWhenFamilyHasNoActiveSession() {
		when(loadSessionPort.loadActiveByFamily(FAMILY_ID)).thenReturn(Optional.empty());

		assertThat(activePickupQueryService.activeChildLocation(FAMILY_ID)).isEmpty();
	}

	@Test
	@DisplayName("진행 중 픽업 확인 activeChildLocation은 진행 중 세션의 어느 자녀도 위치를 보내지 않았으면 빈 값을 돌려준다")
	void activeChildLocationIsEmptyWhenNoChildHasLocation() {
		when(loadSessionPort.loadActiveByFamily(FAMILY_ID))
				.thenReturn(Optional.of(session(SessionStatus.ACTIVE, noLocation())));

		assertThat(activePickupQueryService.activeChildLocation(FAMILY_ID)).isEmpty();
	}

	private static List<SessionChild> noLocation() {
		return List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	// 합류 안내를 받은 민준만 위치를 보냈고 서윤은 아직 보내지 않았다
	private static List<SessionChild> minjunLocatedSeoyunNot() {
		return List.of(
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
						null, null),
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						MINJUN_LOCATION, NOW.minusSeconds(5), null, null));
	}

	private static PickupSession session(SessionStatus status, List<SessionChild> children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, status, VehiclePhase.APPROACHING,
				SpotStatus.CONFIRMED, 8, 0, 1, 0, false, NOW.plus(Duration.ofMinutes(2)), 600,
				NOW.minusSeconds(10), null, null, null, null, null, null, null, null, children);
	}

}

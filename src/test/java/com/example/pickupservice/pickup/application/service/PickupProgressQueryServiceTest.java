package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.in.PickupProgress;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PickupProgressQueryServiceTest {

	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 16);

	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long OTHER_FAMILY_ID = 13L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long SPOT_ID = 21L;

	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

	private PickupProgressQueryService pickupProgressQueryService;

	@BeforeEach
	void setUp() {
		pickupProgressQueryService = new PickupProgressQueryService(loadSessionPort);
	}

	@ParameterizedTest(name = "sessionStatus {0}")
	@EnumSource(value = SessionStatus.class, names = {"ACTIVE", "ON_HOLD"})
	@DisplayName("오늘 픽업 진행 findProgressBySchedule은 진행 중(ACTIVE · ON_HOLD) 세션에 든 일정마다 IN_PROGRESS를 돌려준다")
	void findProgressByScheduleMapsActiveOrOnHoldSessionToInProgress(SessionStatus status) {
		given(loadSessionPort.loadByFamilyAndDate(FAMILY_ID, TODAY_KST))
				.willReturn(List.of(session(901L, status, MINJUN_SCHEDULE_ID, SEOYUN_SCHEDULE_ID)));

		Map<Long, PickupProgress> progress = pickupProgressQueryService.findProgressBySchedule(FAMILY_ID, TODAY_KST);

		assertThat(progress).containsExactlyInAnyOrderEntriesOf(Map.of(
				MINJUN_SCHEDULE_ID, PickupProgress.IN_PROGRESS,
				SEOYUN_SCHEDULE_ID, PickupProgress.IN_PROGRESS));
	}

	@Test
	@DisplayName("오늘 픽업 진행 findProgressBySchedule은 COMPLETED 세션에 든 일정에 COMPLETED를 돌려준다")
	void findProgressByScheduleMapsCompletedSessionToCompleted() {
		given(loadSessionPort.loadByFamilyAndDate(FAMILY_ID, TODAY_KST))
				.willReturn(List.of(session(901L, SessionStatus.COMPLETED, MINJUN_SCHEDULE_ID)));

		Map<Long, PickupProgress> progress = pickupProgressQueryService.findProgressBySchedule(FAMILY_ID, TODAY_KST);

		assertThat(progress).containsExactlyInAnyOrderEntriesOf(Map.of(
				MINJUN_SCHEDULE_ID, PickupProgress.COMPLETED));
	}

	@Test
	@DisplayName("오늘 픽업 진행 findProgressBySchedule은 CANCELLED 세션에만 든 일정을 결과에 넣지 않는다")
	void findProgressByScheduleExcludesSchedulesOnlyInCancelledSessions() {
		given(loadSessionPort.loadByFamilyAndDate(FAMILY_ID, TODAY_KST)).willReturn(List.of(
				session(901L, SessionStatus.CANCELLED, MINJUN_SCHEDULE_ID),
				session(902L, SessionStatus.ACTIVE, SEOYUN_SCHEDULE_ID)));

		Map<Long, PickupProgress> progress = pickupProgressQueryService.findProgressBySchedule(FAMILY_ID, TODAY_KST);

		assertThat(progress).containsExactlyInAnyOrderEntriesOf(Map.of(
				SEOYUN_SCHEDULE_ID, PickupProgress.IN_PROGRESS));
	}

	@ParameterizedTest(name = "{0}({1}) 다음 {2}({3}) → {4}")
	@CsvSource({
			"CANCELLED, 901, ACTIVE,    902, IN_PROGRESS",
			"ACTIVE,    901, CANCELLED, 902, IN_PROGRESS",
			"COMPLETED, 901, ACTIVE,    902, COMPLETED",
			"ACTIVE,    901, COMPLETED, 902, COMPLETED",
			"ON_HOLD,   901, COMPLETED, 902, COMPLETED",
			"COMPLETED, 901, ON_HOLD,   902, COMPLETED",
			"CANCELLED, 901, COMPLETED, 902, COMPLETED",
			"COMPLETED, 901, CANCELLED, 902, COMPLETED"
	})
	@DisplayName("오늘 픽업 진행 findProgressBySchedule은 한 일정이 여러 세션에 들어 있으면 순서와 관계없이 COMPLETED > IN_PROGRESS로 고른다")
	void findProgressBySchedulePrefersCompletedOverInProgressAcrossSessions(SessionStatus first, long firstId,
			SessionStatus second, long secondId, PickupProgress expected) {
		given(loadSessionPort.loadByFamilyAndDate(FAMILY_ID, TODAY_KST)).willReturn(List.of(
				session(firstId, first, MINJUN_SCHEDULE_ID),
				session(secondId, second, MINJUN_SCHEDULE_ID)));

		Map<Long, PickupProgress> progress = pickupProgressQueryService.findProgressBySchedule(FAMILY_ID, TODAY_KST);

		assertThat(progress).containsExactlyInAnyOrderEntriesOf(Map.of(MINJUN_SCHEDULE_ID, expected));
	}

	@Test
	@DisplayName("오늘 픽업 진행 findProgressBySchedule은 그 날짜 그 가족의 세션이 없으면 빈 Map을 돌려준다")
	void findProgressByScheduleReturnsEmptyMapWhenFamilyHasNoSessionOnDate() {
		given(loadSessionPort.loadByFamilyAndDate(FAMILY_ID, TODAY_KST)).willReturn(List.of());

		assertThat(pickupProgressQueryService.findProgressBySchedule(FAMILY_ID, TODAY_KST)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("오늘 픽업 진행 findProgressBySchedule은 받은 familyId · date로만 세션을 읽어 다른 날짜 · 다른 가족 세션을 섞지 않는다")
	void findProgressByScheduleReadsSessionsOnlyOfGivenFamilyAndDate() {
		given(loadSessionPort.loadByFamilyAndDate(FAMILY_ID, TODAY_KST))
				.willReturn(List.of(session(901L, SessionStatus.ACTIVE, MINJUN_SCHEDULE_ID)));
		lenient().when(loadSessionPort.loadByFamilyAndDate(FAMILY_ID, TODAY_KST.minusDays(1)))
				.thenReturn(List.of(session(900L, SessionStatus.COMPLETED, MINJUN_SCHEDULE_ID)));
		lenient().when(loadSessionPort.loadByFamilyAndDate(OTHER_FAMILY_ID, TODAY_KST))
				.thenReturn(List.of(session(903L, SessionStatus.COMPLETED, SEOYUN_SCHEDULE_ID)));

		Map<Long, PickupProgress> progress = pickupProgressQueryService.findProgressBySchedule(FAMILY_ID, TODAY_KST);

		assertThat(progress).containsExactlyInAnyOrderEntriesOf(Map.of(
				MINJUN_SCHEDULE_ID, PickupProgress.IN_PROGRESS));
	}

	private static PickupSession session(long sessionId, SessionStatus status, long... scheduleIds) {
		List<SessionChild> children = LongStream.of(scheduleIds)
				.mapToObj(scheduleId -> SessionChild.restore(sessionId * 10 + scheduleId, childOf(scheduleId),
						scheduleId, ChildPhase.WAITING, 0, null, null, null, null, null))
				.toList();
		return PickupSession.restore(sessionId, VEHICLE_ID, FAMILY_ID, SPOT_ID, "정문 앞", STOP_POINT, MEETING_POINT,
				TODAY_KST, status, VehiclePhase.ROUTING, SpotStatus.SELECTED, 0, 0, 0, null, false, null, null,
				null, null, null, null, null, null, null, null, null, children);
	}

	private static long childOf(long scheduleId) {
		return scheduleId == MINJUN_SCHEDULE_ID ? MINJUN_ID : SEOYUN_ID;
	}

}

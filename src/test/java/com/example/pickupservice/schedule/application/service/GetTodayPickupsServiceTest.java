package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.TodayPickup;
import com.example.pickupservice.schedule.application.port.in.TodayPickupStatus;
import com.example.pickupservice.schedule.application.port.out.LoadPickupProgressPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetTodayPickupsServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long GUARDIAN_ID = 100L;
	private static final long MINJUN_ID = 101L;
	private static final long SEOYEON_ID = 102L;
	private static final long OTHER_FAMILY_CHILD_ID = 199L;
	private static final long ENGLISH_ACADEMY_ID = 301L;
	private static final long PIANO_ACADEMY_ID = 302L;
	private static final long MATH_ACADEMY_ID = 303L;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);

	// UTC 2026-11-15(일) 15:30 = 한국 2026-11-16(월) 00:30
	private static final Instant KST_MONDAY_UTC_SUNDAY = Instant.parse("2026-11-15T15:30:00Z");
	// 한국 2026-11-16(월) 12:00
	private static final Instant KST_MONDAY_NOON = Instant.parse("2026-11-16T03:00:00Z");
	private static final LocalDate MONDAY = LocalDate.of(2026, 11, 16);

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadSchedulePort loadSchedulePort;

	@Mock
	private LoadPickupProgressPort loadPickupProgressPort;

	@Test
	@DisplayName("오늘의 픽업은 한국 날짜 오늘 요일이 들어 있는 가족 자녀들의 일정만 돌려주고 DRAFT 일정도 넣는다")
	void getTodayPickupsReturnsOnlySchedulesOfTodayWeekdayIncludingDraft() {
		givenFamilyMembers();
		givenSchedules(
				schedule(801L, MINJUN_ID, academy(ENGLISH_ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY),
				schedule(802L, SEOYEON_ID, academy(PIANO_ACADEMY_ID, "다솜피아노"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(16, 0), 10, ScheduleStatus.DRAFT),
				schedule(803L, MINJUN_ID, academy(MATH_ACADEMY_ID, "한빛수학"),
						Set.of(DayOfWeek.TUESDAY, DayOfWeek.SUNDAY), LocalTime.of(17, 0), 0, ScheduleStatus.READY));
		givenProgress(Map.of());

		List<TodayPickup> pickups = service(KST_MONDAY_NOON).getTodayPickups(FAMILY_ID);

		assertThat(pickups).extracting(TodayPickup::scheduleId).containsExactlyInAnyOrder(801L, 802L);
	}

	@Test
	@DisplayName("오늘의 픽업은 scheduleId · childName · academyName · targetAt(오늘 한국 날짜의 수업 끝 시각 + targetPickup분)을 담는다")
	void getTodayPickupsFillsChildNameAcademyNameAndKstTargetAt() {
		givenFamilyMembers();
		givenSchedules(schedule(801L, MINJUN_ID, academy(ENGLISH_ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenProgress(Map.of());

		List<TodayPickup> pickups = service(KST_MONDAY_NOON).getTodayPickups(FAMILY_ID);

		assertThat(pickups).containsExactly(new TodayPickup(801L, "민준", "해솔영어학원",
				kst("2026-11-16T18:05:00+09:00"), TodayPickupStatus.SCHEDULED));
	}

	@Test
	@DisplayName("오늘의 픽업은 targetAt 오름차순이고 targetAt이 같으면 scheduleId 오름차순이다")
	void getTodayPickupsIsSortedByTargetAtThenScheduleId() {
		givenFamilyMembers();
		givenSchedules(
				schedule(805L, MINJUN_ID, academy(ENGLISH_ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY),
				schedule(804L, SEOYEON_ID, academy(MATH_ACADEMY_ID, "한빛수학"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY),
				schedule(806L, SEOYEON_ID, academy(PIANO_ACADEMY_ID, "다솜피아노"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(15, 50), 10, ScheduleStatus.READY),
				schedule(803L, MINJUN_ID, academy(MATH_ACADEMY_ID, "한빛수학"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(19, 0), 0, ScheduleStatus.READY));
		givenProgress(Map.of());

		List<TodayPickup> pickups = service(KST_MONDAY_NOON).getTodayPickups(FAMILY_ID);

		assertThat(pickups).extracting(TodayPickup::scheduleId, TodayPickup::targetAt).containsExactly(
				tuple(806L, kst("2026-11-16T16:00:00+09:00")),
				tuple(804L, kst("2026-11-16T18:05:00+09:00")),
				tuple(805L, kst("2026-11-16T18:05:00+09:00")),
				tuple(803L, kst("2026-11-16T19:00:00+09:00")));
	}

	@Test
	@DisplayName("오늘의 픽업은 LoadPickupProgressPort가 준 일정별 진행 상태(IN_PROGRESS · COMPLETED)를 status로 쓰고 맵에 없는 일정은 SCHEDULED다")
	void getTodayPickupsUsesProgressStatusAndDefaultsToScheduled() {
		givenFamilyMembers();
		givenSchedules(
				schedule(801L, MINJUN_ID, academy(ENGLISH_ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), 0, ScheduleStatus.READY),
				schedule(802L, SEOYEON_ID, academy(PIANO_ACADEMY_ID, "다솜피아노"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(16, 0), 0, ScheduleStatus.READY),
				schedule(803L, MINJUN_ID, academy(MATH_ACADEMY_ID, "한빛수학"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 0, ScheduleStatus.DRAFT));
		givenProgress(Map.of(
				801L, TodayPickupStatus.COMPLETED,
				802L, TodayPickupStatus.IN_PROGRESS));

		List<TodayPickup> pickups = service(KST_MONDAY_NOON).getTodayPickups(FAMILY_ID);

		assertThat(pickups).extracting(TodayPickup::scheduleId, TodayPickup::status).containsExactly(
				tuple(801L, TodayPickupStatus.COMPLETED),
				tuple(802L, TodayPickupStatus.IN_PROGRESS),
				tuple(803L, TodayPickupStatus.SCHEDULED));
	}

	@Test
	@DisplayName("오늘의 픽업은 UTC로는 전날(일요일)이어도 한국 날짜(월요일) 요일의 일정을 고르고 진행 상태도 한국 날짜로 읽는다")
	void getTodayPickupsUsesKstDateWhenUtcDateIsPreviousDay() {
		givenFamilyMembers();
		givenSchedules(
				schedule(801L, MINJUN_ID, academy(ENGLISH_ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY),
				schedule(809L, SEOYEON_ID, academy(PIANO_ACADEMY_ID, "다솜피아노"),
						Set.of(DayOfWeek.SUNDAY), LocalTime.of(16, 0), 0, ScheduleStatus.READY));
		given(loadPickupProgressPort.loadProgress(FAMILY_ID, MONDAY))
				.willReturn(Map.of(801L, TodayPickupStatus.IN_PROGRESS));

		List<TodayPickup> pickups = service(KST_MONDAY_UTC_SUNDAY).getTodayPickups(FAMILY_ID);

		assertThat(pickups).containsExactly(new TodayPickup(801L, "민준", "해솔영어학원",
				kst("2026-11-16T18:05:00+09:00"), TodayPickupStatus.IN_PROGRESS));
		then(loadPickupProgressPort).should().loadProgress(FAMILY_ID, MONDAY);
	}

	@Test
	@DisplayName("오늘의 픽업은 그 가족 CHILD들의 userId로만 일정을 읽는다")
	void getTodayPickupsLoadsSchedulesOfFamilyChildrenOnly() {
		givenFamilyMembers();
		given(loadSchedulePort.loadSchedulesOfChildren(anyList())).willReturn(List.of());
		givenProgress(Map.of());

		service(KST_MONDAY_NOON).getTodayPickups(FAMILY_ID);

		then(loadSchedulePort).should().loadSchedulesOfChildren(argThat(ids ->
				ids != null && ids.size() == 2 && ids.containsAll(List.of(MINJUN_ID, SEOYEON_ID))
						&& !ids.contains(GUARDIAN_ID) && !ids.contains(OTHER_FAMILY_CHILD_ID)));
	}

	@Test
	@DisplayName("오늘의 픽업은 오늘 요일 일정이 없으면 빈 목록을 돌려준다")
	void getTodayPickupsReturnsEmptyListWhenNoScheduleToday() {
		givenFamilyMembers();
		givenSchedules(schedule(803L, MINJUN_ID, academy(MATH_ACADEMY_ID, "한빛수학"),
				Set.of(DayOfWeek.TUESDAY), LocalTime.of(17, 0), 0, ScheduleStatus.READY));
		givenProgress(Map.of());

		assertThat(service(KST_MONDAY_NOON).getTodayPickups(FAMILY_ID)).isNotNull().isEmpty();
	}

	private GetTodayPickupsService service(Instant now) {
		return new GetTodayPickupsService(loadMemberUseCase, loadSchedulePort, loadPickupProgressPort,
				Clock.fixed(now, ZoneOffset.UTC));
	}

	private void givenFamilyMembers() {
		given(loadMemberUseCase.findMembers(FAMILY_ID)).willReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1111-2222"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", "010-3333-4444"),
				new MemberInfo(SEOYEON_ID, FAMILY_ID, Role.CHILD, "서연", "010-5555-6666")));
	}

	private void givenSchedules(PickupSchedule... schedules) {
		given(loadSchedulePort.loadSchedulesOfChildren(anyList())).willReturn(List.of(schedules));
	}

	private void givenProgress(Map<Long, TodayPickupStatus> progress) {
		given(loadPickupProgressPort.loadProgress(anyLong(), any(LocalDate.class))).willReturn(progress);
	}

	private static Instant kst(String offsetDateTime) {
		return OffsetDateTime.parse(offsetDateTime).toInstant();
	}

	private static Academy academy(long academyId, String name) {
		return Academy.restore(academyId, FAMILY_ID, "kakao:" + academyId, name, "서울 강남구 테헤란로 152", LOCATION,
				ENTRANCE, "02-555-1234");
	}

	private static PickupSchedule schedule(long scheduleId, long childId, Academy academy, Set<DayOfWeek> weekdays,
			LocalTime classEndTime, int targetPickup, ScheduleStatus status) {
		return PickupSchedule.restore(scheduleId, childId, academy, weekdays, classEndTime, targetPickup, status);
	}
}

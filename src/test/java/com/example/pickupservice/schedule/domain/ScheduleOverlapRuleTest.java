package com.example.pickupservice.schedule.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduleOverlapRuleTest {

	private static final long FAMILY_ID = 3L;
	private static final long CHILD_USER_ID = 7L;
	private static final long OTHER_CHILD_USER_ID = 8L;
	private static final LocalTime SIX_PM = LocalTime.of(18, 0);
	private static final LocalTime FIVE_PM = LocalTime.of(17, 0);

	@Test
	@DisplayName("겹침 규칙은 같은 자녀 · 같은 요일 · 같은 classEndTime 일정을 겹침으로 판정한다")
	void findsExistingScheduleWithSameChildWeekdayAndClassEndTime() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), SIX_PM);
		List<PickupSchedule> existing = List.of(existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY), SIX_PM));

		assertThat(ScheduleOverlapRule.findOverlapScheduleIds(candidate, existing)).containsExactly(10L);
	}

	@Test
	@DisplayName("겹침 규칙은 다른 자녀의 같은 요일 · 같은 시각 일정을 겹침으로 보지 않는다")
	void ignoresScheduleOfOtherChild() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), SIX_PM);
		List<PickupSchedule> existing = List.of(existingSchedule(10L, OTHER_CHILD_USER_ID, Set.of(MONDAY), SIX_PM));

		assertThat(ScheduleOverlapRule.findOverlapScheduleIds(candidate, existing)).isEmpty();
	}

	@Test
	@DisplayName("겹침 규칙은 같은 자녀 · 같은 시각이어도 요일이 다르면 겹침으로 보지 않는다")
	void ignoresScheduleOnOtherWeekday() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY, WEDNESDAY), SIX_PM);
		List<PickupSchedule> existing = List.of(existingSchedule(10L, CHILD_USER_ID, Set.of(TUESDAY, FRIDAY), SIX_PM));

		assertThat(ScheduleOverlapRule.findOverlapScheduleIds(candidate, existing)).isEmpty();
	}

	@Test
	@DisplayName("겹침 규칙은 같은 자녀 · 같은 요일이어도 classEndTime이 다르면 겹침으로 보지 않는다")
	void ignoresScheduleWithOtherClassEndTime() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), SIX_PM);
		List<PickupSchedule> existing = List.of(existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY), FIVE_PM));

		assertThat(ScheduleOverlapRule.findOverlapScheduleIds(candidate, existing)).isEmpty();
	}

	@Test
	@DisplayName("겹침 규칙은 여러 요일 중 하나만 겹쳐도 겹침으로 판정한다")
	void findsOverlapWhenOnlyOneOfWeekdaysMatches() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY, WEDNESDAY, FRIDAY), SIX_PM);
		List<PickupSchedule> existing = List.of(existingSchedule(10L, CHILD_USER_ID, Set.of(TUESDAY, FRIDAY), SIX_PM));

		assertThat(ScheduleOverlapRule.findOverlapScheduleIds(candidate, existing)).containsExactly(10L);
	}

	@Test
	@DisplayName("겹침 규칙은 겹치는 기존 일정만 골라 scheduleId를 모두 돌려준다")
	void returnsOnlyOverlappingScheduleIds() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY, WEDNESDAY), SIX_PM);
		List<PickupSchedule> existing = List.of(
				existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY), SIX_PM),
				existingSchedule(11L, CHILD_USER_ID, Set.of(MONDAY), FIVE_PM),
				existingSchedule(12L, OTHER_CHILD_USER_ID, Set.of(WEDNESDAY), SIX_PM),
				existingSchedule(13L, CHILD_USER_ID, Set.of(WEDNESDAY), SIX_PM),
				existingSchedule(14L, CHILD_USER_ID, Set.of(TUESDAY), SIX_PM));

		assertThat(ScheduleOverlapRule.findOverlapScheduleIds(candidate, existing))
				.containsExactlyInAnyOrder(10L, 13L);
	}

	@Test
	@DisplayName("겹침 규칙은 기존 일정이 없으면 빈 목록을 돌려준다")
	void returnsEmptyWhenNoExistingSchedules() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), SIX_PM);

		assertThat(ScheduleOverlapRule.findOverlapScheduleIds(candidate, List.of())).isEmpty();
	}

	private static PickupSchedule newSchedule(long childUserId, Set<DayOfWeek> weekdays, LocalTime classEndTime) {
		return PickupSchedule.create(childUserId, academy(), weekdays, classEndTime, 5);
	}

	private static PickupSchedule existingSchedule(long scheduleId, long childUserId, Set<DayOfWeek> weekdays,
			LocalTime classEndTime) {
		return PickupSchedule.restore(scheduleId, childUserId, academy(), weekdays, classEndTime, 5,
				ScheduleStatus.READY);
	}

	private static Academy academy() {
		return Academy.create(FAMILY_ID, "kakao:12345", "한빛수학학원", "서울 중구 세종대로 110", new LatLng(37.5665, 126.9780),
				new LatLng(37.5666, 126.9781), "+8221234567");
	}
}

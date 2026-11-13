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

class DailyScheduleLimitRuleTest {

	private static final long FAMILY_ID = 3L;
	private static final long CHILD_USER_ID = 7L;
	private static final long OTHER_CHILD_USER_ID = 8L;

	@Test
	@DisplayName("하루 한도 규칙은 같은 자녀 같은 요일에 네 번째 일정이면 한도 초과로 판정한다")
	void exceedsLimitWhenFourthScheduleOnSameWeekday() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(19, 0));
		List<PickupSchedule> existing = List.of(
				existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(15, 0)),
				existingSchedule(11L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(16, 0)),
				existingSchedule(12L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(17, 0)));

		assertThat(DailyScheduleLimitRule.exceedsLimit(candidate, existing)).isTrue();
	}

	@Test
	@DisplayName("하루 한도 규칙은 같은 자녀 같은 요일에 세 번째 일정이면 한도 안으로 판정한다")
	void withinLimitWhenThirdScheduleOnSameWeekday() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(19, 0));
		List<PickupSchedule> existing = List.of(
				existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(15, 0)),
				existingSchedule(11L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(16, 0)));

		assertThat(DailyScheduleLimitRule.exceedsLimit(candidate, existing)).isFalse();
	}

	@Test
	@DisplayName("하루 한도 규칙은 다른 요일 일정을 세지 않는다")
	void doesNotCountSchedulesOnOtherWeekdays() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(19, 0));
		List<PickupSchedule> existing = List.of(
				existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(15, 0)),
				existingSchedule(11L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(16, 0)),
				existingSchedule(12L, CHILD_USER_ID, Set.of(TUESDAY), LocalTime.of(17, 0)),
				existingSchedule(13L, CHILD_USER_ID, Set.of(WEDNESDAY, FRIDAY), LocalTime.of(17, 0)));

		assertThat(DailyScheduleLimitRule.exceedsLimit(candidate, existing)).isFalse();
	}

	@Test
	@DisplayName("하루 한도 규칙은 다른 자녀의 같은 요일 일정을 세지 않는다")
	void doesNotCountSchedulesOfOtherChild() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(19, 0));
		List<PickupSchedule> existing = List.of(
				existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(15, 0)),
				existingSchedule(11L, CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(16, 0)),
				existingSchedule(12L, OTHER_CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(17, 0)));

		assertThat(DailyScheduleLimitRule.exceedsLimit(candidate, existing)).isFalse();
	}

	@Test
	@DisplayName("하루 한도 규칙은 새 일정의 여러 요일 중 한 요일만 네 번째여도 한도 초과로 판정한다")
	void exceedsLimitWhenOnlyOneOfWeekdaysIsFull() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY, WEDNESDAY), LocalTime.of(19, 0));
		List<PickupSchedule> existing = List.of(
				existingSchedule(10L, CHILD_USER_ID, Set.of(MONDAY, WEDNESDAY), LocalTime.of(15, 0)),
				existingSchedule(11L, CHILD_USER_ID, Set.of(WEDNESDAY), LocalTime.of(16, 0)),
				existingSchedule(12L, CHILD_USER_ID, Set.of(WEDNESDAY, FRIDAY), LocalTime.of(17, 0)));

		assertThat(DailyScheduleLimitRule.exceedsLimit(candidate, existing)).isTrue();
	}

	@Test
	@DisplayName("하루 한도 규칙은 기존 일정이 없으면 한도 안으로 판정한다")
	void withinLimitWhenNoExistingSchedules() {
		PickupSchedule candidate = newSchedule(CHILD_USER_ID, Set.of(MONDAY), LocalTime.of(19, 0));

		assertThat(DailyScheduleLimitRule.exceedsLimit(candidate, List.of())).isFalse();
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

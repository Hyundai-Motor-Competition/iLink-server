package com.example.pickupservice.schedule.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickupScheduleTest {

	private static final long CHILD_USER_ID = 7L;
	private static final LatLng LOCATION = new LatLng(37.5665, 126.9780);
	private static final LatLng ENTRANCE = new LatLng(37.5666, 126.9781);
	private static final Set<DayOfWeek> MON_WED_FRI =
			Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
	private static final LocalTime SIX_PM = LocalTime.of(18, 0);
	private static final Path SOURCE =
			Path.of("src/main/java/com/example/pickupservice/schedule/domain/PickupSchedule.java");

	@Test
	@DisplayName("학원 출입구가 없으면 PickupSchedule은 DRAFT로 만들어진다")
	void createsDraftScheduleWhenAcademyHasNoEntrance() {
		PickupSchedule schedule = PickupSchedule.create(CHILD_USER_ID, academyWithoutEntrance(), MON_WED_FRI, SIX_PM, 5);

		assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
	}

	@Test
	@DisplayName("학원 출입구가 있으면 PickupSchedule은 READY로 만들어진다")
	void createsReadyScheduleWhenAcademyHasEntrance() {
		PickupSchedule schedule = PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), MON_WED_FRI, SIX_PM, 5);

		assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.READY);
	}

	@Test
	@DisplayName("새로 만든 PickupSchedule은 받은 자녀 · 학원 · 요일 · 수업 종료 시각 · targetPickup을 그대로 가진다")
	void createKeepsGivenValues() {
		Academy academy = academyWithEntrance();

		PickupSchedule schedule = PickupSchedule.create(CHILD_USER_ID, academy, MON_WED_FRI, SIX_PM, 5);

		assertThat(schedule.getScheduleId()).isNull();
		assertThat(schedule.getChildUserId()).isEqualTo(CHILD_USER_ID);
		assertThat(schedule.getAcademy()).isSameAs(academy);
		assertThat(schedule.getWeekdays()).containsExactlyInAnyOrderElementsOf(MON_WED_FRI);
		assertThat(schedule.getClassEndTime()).isEqualTo(SIX_PM);
		assertThat(schedule.getTargetPickup()).isEqualTo(5);
	}

	@ParameterizedTest(name = "[{index}] targetPickup = {0}")
	@ValueSource(ints = {-1, 61, Integer.MIN_VALUE, Integer.MAX_VALUE})
	@DisplayName("일정 규칙은 targetPickup이 0~60을 벗어나면 도메인 예외 InvalidScheduleException(field targetPickup)으로 거절한다")
	void rejectsTargetPickupOutOfRange(int targetPickup) {
		assertInvalidSchedule(
				() -> PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), MON_WED_FRI, SIX_PM, targetPickup),
				"targetPickup", "0~60이어야 합니다");
	}

	@ParameterizedTest(name = "[{index}] targetPickup = {0}")
	@ValueSource(ints = {0, 5, 60})
	@DisplayName("일정 규칙은 targetPickup 경계값 0 · 60과 그 사이 값을 받는다")
	void acceptsTargetPickupWithinRange(int targetPickup) {
		PickupSchedule schedule =
				PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), MON_WED_FRI, SIX_PM, targetPickup);

		assertThat(schedule.getTargetPickup()).isEqualTo(targetPickup);
	}

	@Test
	@DisplayName("일정 규칙은 weekdays가 비어 있으면 도메인 예외 InvalidScheduleException(field weekdays)으로 거절한다")
	void rejectsEmptyWeekdays() {
		assertInvalidSchedule(
				() -> PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), Set.of(), SIX_PM, 5),
				"weekdays", "1개 이상이어야 합니다");
	}

	@Test
	@DisplayName("PickupSchedule 소스는 Spring과 common.error를 import하지 않는다")
	void sourceHasNoSpringOrCommonErrorImport() throws IOException {
		String source = Files.readString(SOURCE, StandardCharsets.UTF_8);

		assertThat(source).doesNotContain("org.springframework").doesNotContain("common.error");
	}

	@Test
	@DisplayName("일정 규칙은 weekdays가 하루뿐이어도 받는다")
	void acceptsSingleWeekday() {
		PickupSchedule schedule =
				PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), Set.of(DayOfWeek.SUNDAY), SIX_PM, 5);

		assertThat(schedule.getWeekdays()).containsExactly(DayOfWeek.SUNDAY);
	}

	@Test
	@DisplayName("targetPickupTime 계산은 classEndTime 18:00 · targetPickup 5에서 18:05를 돌려준다")
	void targetPickupTimeAddsTargetPickupMinutesToClassEndTime() {
		PickupSchedule schedule = PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), MON_WED_FRI, SIX_PM, 5);

		assertThat(schedule.targetPickupTime()).isEqualTo(LocalTime.of(18, 5));
	}

	@Test
	@DisplayName("targetPickupTime 계산은 targetPickup 0이면 classEndTime을 그대로 돌려준다")
	void targetPickupTimeEqualsClassEndTimeWhenTargetPickupIsZero() {
		PickupSchedule schedule = PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), MON_WED_FRI, SIX_PM, 0);

		assertThat(schedule.targetPickupTime()).isEqualTo(SIX_PM);
	}

	@Test
	@DisplayName("targetPickupTime 계산은 classEndTime 17:30 · targetPickup 60에서 18:30을 돌려준다")
	void targetPickupTimeCrossesHour() {
		PickupSchedule schedule =
				PickupSchedule.create(CHILD_USER_ID, academyWithEntrance(), MON_WED_FRI, LocalTime.of(17, 30), 60);

		assertThat(schedule.targetPickupTime()).isEqualTo(LocalTime.of(18, 30));
	}

	private static Academy academyWithEntrance() {
		return Academy.create("kakao:12345", "한빛수학학원", "서울 중구 세종대로 110", LOCATION, ENTRANCE, "+8221234567");
	}

	private static Academy academyWithoutEntrance() {
		return Academy.create("kakao:12345", "한빛수학학원", "서울 중구 세종대로 110", LOCATION, null, "+8221234567");
	}

	private static void assertInvalidSchedule(ThrowingCallable call, String field, String reason) {
		assertThatThrownBy(call).isInstanceOfSatisfying(InvalidScheduleException.class, e -> {
			assertThat(e.field()).isEqualTo(field);
			assertThat(e.reason()).isEqualTo(reason);
		});
	}
}

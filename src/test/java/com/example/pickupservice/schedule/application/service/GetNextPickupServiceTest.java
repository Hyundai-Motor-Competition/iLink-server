package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.NextPickup;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetNextPickupServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long CHILD_ID = 101L;
	private static final long ACADEMY_ID = 301L;
	private static final long OTHER_ACADEMY_ID = 302L;
	private static final long SPOT_ID = 701L;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);

	// 2026-11-16은 월요일, 2026-11-15는 일요일이다. 시각은 모두 한국 시간(+09:00)으로 적는다.

	@Mock
	private LoadSchedulePort loadSchedulePort;

	@Mock
	private LoadPickupSpotPort loadPickupSpotPort;

	// #79

	@Test
	@DisplayName("다음 픽업은 오늘 아직 지나지 않은 READY 일정의 학원 이름 · 기본 장소 이름 · targetAt(classEndTime + targetPickup분)을 돌려준다")
	void getNextPickupReturnsTodayScheduleNotYetPassed() {
		givenSchedules(schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenSpots(new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true));

		Optional<NextPickup> next = service(kst("2026-11-16T17:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).contains(new NextPickup("해솔영어학원", "정문 앞", kst("2026-11-16T18:05:00")));
	}

	@Test
	@DisplayName("다음 픽업은 그 학원 하나만 담아 가족의 장소를 조회한다(loadSpots(familyId, [academyId]))")
	void getNextPickupLoadsSpotsOfChosenAcademyOnly() {
		givenSchedules(schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenSpots(new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true));

		service(kst("2026-11-16T17:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		verify(loadSchedulePort).loadSchedulesOfChild(CHILD_ID);
		verify(loadPickupSpotPort).loadSpots(FAMILY_ID, List.of(ACADEMY_ID));
	}

	@Test
	@DisplayName("다음 픽업은 오늘 일정의 targetAt이 지났으면 다음 날 일정을 돌려준다")
	void getNextPickupMovesToNextDayWhenTodayTargetHasPassed() {
		givenSchedules(schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenSpots(new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true));

		Optional<NextPickup> next = service(kst("2026-11-16T18:10:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).map(NextPickup::targetAt).contains(kst("2026-11-17T18:05:00"));
	}

	@Test
	@DisplayName("다음 픽업은 targetAt이 지금과 같으면 지난 것으로 보고 다음 픽업을 돌려준다")
	void getNextPickupTreatsTargetAtEqualToNowAsPassed() {
		givenSchedules(schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenSpots(new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true));

		Optional<NextPickup> next = service(kst("2026-11-16T18:05:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).map(NextPickup::targetAt).contains(kst("2026-11-17T18:05:00"));
	}

	@Test
	@DisplayName("다음 픽업은 여러 일정 중 targetAt(classEndTime + targetPickup분)이 가장 이른 것을 돌려준다")
	void getNextPickupReturnsEarliestTargetAtAmongSchedules() {
		givenSchedules(
				schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 30, ScheduleStatus.READY),
				schedule(802L, academy(OTHER_ACADEMY_ID, "한빛수학학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 20), 0, ScheduleStatus.READY),
				schedule(803L, academy(OTHER_ACADEMY_ID, "한빛수학학원"),
						Set.of(DayOfWeek.TUESDAY), LocalTime.of(15, 0), 0, ScheduleStatus.READY));
		givenSpots();

		Optional<NextPickup> next = service(kst("2026-11-16T10:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).map(NextPickup::academyName).contains("한빛수학학원");
		assertThat(next).map(NextPickup::targetAt).contains(kst("2026-11-16T18:20:00"));
	}

	@Test
	@DisplayName("다음 픽업은 오늘 요일 일정이 지났고 다른 요일 일정이 없으면 다음 주 같은 요일을 돌려준다")
	void getNextPickupMovesToSameWeekdayNextWeekWhenOnlyTodayWeekdayExists() {
		givenSchedules(schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenSpots(new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true));

		Optional<NextPickup> next = service(kst("2026-11-16T19:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).map(NextPickup::targetAt).contains(kst("2026-11-23T18:05:00"));
	}

	@Test
	@DisplayName("다음 픽업은 일요일 일정이 지났으면 주를 넘어 월요일 일정을 돌려준다")
	void getNextPickupCrossesFromSundayToMonday() {
		givenSchedules(
				schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.SUNDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY),
				schedule(802L, academy(OTHER_ACADEMY_ID, "한빛수학학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), 0, ScheduleStatus.READY));
		givenSpots();

		Optional<NextPickup> next = service(kst("2026-11-15T19:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).map(NextPickup::academyName).contains("한빛수학학원");
		assertThat(next).map(NextPickup::targetAt).contains(kst("2026-11-16T08:00:00"));
	}

	@Test
	@DisplayName("다음 픽업은 UTC로는 일요일이어도 한국 날짜(월요일) 기준으로 요일과 시각을 판정한다")
	void getNextPickupUsesKstDateAndTimeEvenWhenUtcDateIsSunday() {
		// 한국 2026-11-16(월) 01:00 = UTC 2026-11-15(일) 16:00
		givenSchedules(
				schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.SUNDAY), LocalTime.of(20, 0), 0, ScheduleStatus.READY),
				schedule(802L, academy(OTHER_ACADEMY_ID, "한빛수학학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenSpots();

		Optional<NextPickup> next = service(kst("2026-11-16T01:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).map(NextPickup::academyName).contains("한빛수학학원");
		assertThat(next).map(NextPickup::targetAt).contains(kst("2026-11-16T18:05:00"));
	}

	@Test
	@DisplayName("다음 픽업은 DRAFT 일정을 빼고 READY 일정 중에서 고른다")
	void getNextPickupSkipsDraftSchedules() {
		givenSchedules(
				schedule(801L, academyWithoutEntrance(ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.DRAFT),
				schedule(802L, academy(OTHER_ACADEMY_ID, "한빛수학학원"),
						Set.of(DayOfWeek.TUESDAY), LocalTime.of(17, 0), 0, ScheduleStatus.READY));
		givenSpots();

		Optional<NextPickup> next = service(kst("2026-11-16T10:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).map(NextPickup::academyName).contains("한빛수학학원");
		assertThat(next).map(NextPickup::targetAt).contains(kst("2026-11-17T17:00:00"));
	}

	@Test
	@DisplayName("다음 픽업은 DRAFT 일정만 있으면 빈 값을 돌려준다")
	void getNextPickupReturnsEmptyWhenOnlyDraftSchedulesExist() {
		givenSchedules(schedule(801L, academyWithoutEntrance(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.DRAFT));
		givenSpots();

		Optional<NextPickup> next = service(kst("2026-11-16T10:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).isEmpty();
	}

	@Test
	@DisplayName("다음 픽업은 그 학원에 가족의 기본 장소가 없으면 spotName을 null로 돌려준다")
	void getNextPickupReturnsNullSpotNameWhenDefaultSpotIsMissing() {
		givenSchedules(schedule(801L, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenSpots(new SpotSummary(SPOT_ID, ACADEMY_ID, "후문", false));

		Optional<NextPickup> next = service(kst("2026-11-16T17:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).contains(new NextPickup("해솔영어학원", null, kst("2026-11-16T18:05:00")));
	}

	@Test
	@DisplayName("다음 픽업은 자녀의 일정이 하나도 없으면 빈 값을 돌려준다")
	void getNextPickupReturnsEmptyWhenChildHasNoSchedule() {
		givenSchedules();
		givenSpots();

		Optional<NextPickup> next = service(kst("2026-11-16T10:00:00")).getNextPickup(FAMILY_ID, CHILD_ID);

		assertThat(next).isEmpty();
	}

	private GetNextPickupService service(Instant now) {
		return new GetNextPickupService(loadSchedulePort, loadPickupSpotPort, Clock.fixed(now, ZoneOffset.UTC));
	}

	private void givenSchedules(PickupSchedule... schedules) {
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(schedules));
	}

	private void givenSpots(SpotSummary... spots) {
		given(loadPickupSpotPort.loadSpots(eq(FAMILY_ID), anyList())).willReturn(List.of(spots));
	}

	private static Instant kst(String localDateTime) {
		return OffsetDateTime.parse(localDateTime + "+09:00").toInstant();
	}

	private static Academy academy(long academyId, String name) {
		return Academy.restore(academyId, FAMILY_ID, "kakao:" + academyId, name, "서울 강남구 테헤란로 152",
				LOCATION, ENTRANCE, "02-555-1234");
	}

	private static Academy academyWithoutEntrance(long academyId, String name) {
		return Academy.restore(academyId, FAMILY_ID, "kakao:" + academyId, name, "서울 강남구 테헤란로 152",
				LOCATION, null, "02-555-1234");
	}

	private static PickupSchedule schedule(long scheduleId, Academy academy, Set<DayOfWeek> weekdays,
			LocalTime classEndTime, int targetPickup, ScheduleStatus status) {
		return PickupSchedule.restore(scheduleId, CHILD_ID, academy, weekdays, classEndTime, targetPickup, status);
	}
}

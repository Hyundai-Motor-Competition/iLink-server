package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyInfo;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoadScheduleServiceTest {

	private static final long MINJUN_ID = 131L;
	private static final long SEOYEON_ID = 132L;
	private static final long ACADEMY_ID = 301L;
	private static final long OTHER_ACADEMY_ID = 302L;
	private static final long SCHEDULE_ID = 501L;
	private static final long OTHER_SCHEDULE_ID = 502L;
	private static final long MISSING_SCHEDULE_ID = 999L;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);
	private static final LatLng OTHER_LOCATION = new LatLng(37.502000, 127.041000);

	@Mock
	private LoadSchedulePort loadSchedulePort;

	@InjectMocks
	private LoadScheduleService service;

	// #37 · #74

	@Test
	@DisplayName("findSchedules는 scheduleId의 일정 값과 학원 이름 · 주소 · 위치 · 출입구 · 대표번호를 돌려준다")
	void findSchedulesReturnsScheduleWithAcademyInfo() {
		Academy academy = Academy.restore(ACADEMY_ID, "kakao:12345", "해솔영어학원", "서울 강남구 테헤란로 1", LOCATION, ENTRANCE,
				"02-555-1234");
		given(loadSchedulePort.loadSchedules(List.of(SCHEDULE_ID))).willReturn(List.of(PickupSchedule.restore(SCHEDULE_ID,
				MINJUN_ID, academy, Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(18, 0), 5,
				ScheduleStatus.READY)));

		List<ScheduleInfo> schedules = service.findSchedules(List.of(SCHEDULE_ID));

		assertThat(schedules).containsExactly(new ScheduleInfo(SCHEDULE_ID, MINJUN_ID,
				new AcademyInfo(ACADEMY_ID, "해솔영어학원", "서울 강남구 테헤란로 1", LOCATION, ENTRANCE, "02-555-1234"),
				Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
	}

	@Test
	@DisplayName("findSchedules는 loadSchedules를 받은 id 목록 그대로 정확히 1번 부르고 loadSchedule은 부르지 않는다")
	void findSchedulesCallsLoadSchedulesOnceWithRequestedIds() {
		List<Long> requested = List.of(OTHER_SCHEDULE_ID, MISSING_SCHEDULE_ID, SCHEDULE_ID);
		given(loadSchedulePort.loadSchedules(requested)).willReturn(List.of(
				schedule(SCHEDULE_ID, MINJUN_ID, ACADEMY_ID, "해솔영어학원"),
				schedule(OTHER_SCHEDULE_ID, SEOYEON_ID, OTHER_ACADEMY_ID, "한빛수학학원")));

		service.findSchedules(requested);

		verify(loadSchedulePort, times(1)).loadSchedules(requested);
		verify(loadSchedulePort, times(1)).loadSchedules(anyList());
		verify(loadSchedulePort, never()).loadSchedule(anyLong());
	}

	@Test
	@DisplayName("findSchedules는 포트가 다른 순서로 돌려줘도 받은 scheduleId 순서대로 돌려준다")
	void findSchedulesReturnsSchedulesInRequestedOrder() {
		List<Long> requested = List.of(OTHER_SCHEDULE_ID, SCHEDULE_ID);
		given(loadSchedulePort.loadSchedules(requested)).willReturn(List.of(
				schedule(SCHEDULE_ID, MINJUN_ID, ACADEMY_ID, "해솔영어학원"),
				schedule(OTHER_SCHEDULE_ID, SEOYEON_ID, OTHER_ACADEMY_ID, "한빛수학학원")));

		List<ScheduleInfo> schedules = service.findSchedules(requested);

		assertThat(schedules).extracting(ScheduleInfo::scheduleId).containsExactly(OTHER_SCHEDULE_ID, SCHEDULE_ID);
		assertThat(schedules).extracting(info -> info.academy().name()).containsExactly("한빛수학학원", "해솔영어학원");
	}

	@Test
	@DisplayName("findSchedules는 학원 출입구가 없는 DRAFT 일정도 entrance null로 돌려준다")
	void findSchedulesReturnsDraftScheduleWithNullEntrance() {
		Academy academy = Academy.restore(ACADEMY_ID, "kakao:12345", "해솔영어학원", "서울 강남구 테헤란로 1", LOCATION, null,
				"02-555-1234");
		given(loadSchedulePort.loadSchedules(List.of(SCHEDULE_ID))).willReturn(List.of(PickupSchedule.restore(SCHEDULE_ID,
				MINJUN_ID, academy, Set.of(DayOfWeek.FRIDAY), LocalTime.of(17, 30), 0, ScheduleStatus.DRAFT)));

		List<ScheduleInfo> schedules = service.findSchedules(List.of(SCHEDULE_ID));

		assertThat(schedules).hasSize(1);
		assertThat(schedules.get(0).status()).isEqualTo(ScheduleStatus.DRAFT);
		assertThat(schedules.get(0).academy().entrance()).isNull();
		assertThat(schedules.get(0).academy().location()).isEqualTo(LOCATION);
	}

	@Test
	@DisplayName("findSchedules는 포트가 돌려주지 않은 없는 scheduleId를 빼고 나머지만 받은 순서대로 돌려준다")
	void findSchedulesSkipsMissingScheduleIds() {
		List<Long> requested = List.of(OTHER_SCHEDULE_ID, MISSING_SCHEDULE_ID, SCHEDULE_ID);
		given(loadSchedulePort.loadSchedules(requested)).willReturn(List.of(
				schedule(SCHEDULE_ID, MINJUN_ID, ACADEMY_ID, "해솔영어학원"),
				schedule(OTHER_SCHEDULE_ID, SEOYEON_ID, OTHER_ACADEMY_ID, "한빛수학학원")));

		List<ScheduleInfo> schedules = service.findSchedules(requested);

		assertThat(schedules).extracting(ScheduleInfo::scheduleId).containsExactly(OTHER_SCHEDULE_ID, SCHEDULE_ID);
	}

	@Test
	@DisplayName("findSchedules는 빈 목록을 받으면 포트를 부르지 않고 빈 목록을 돌려준다")
	void findSchedulesReturnsEmptyListForEmptyInput() {
		List<ScheduleInfo> schedules = service.findSchedules(List.of());

		assertThat(schedules).isEmpty();
		verify(loadSchedulePort, never()).loadSchedules(anyList());
		verify(loadSchedulePort, never()).loadSchedule(anyLong());
	}

	private static PickupSchedule schedule(long scheduleId, long childUserId, long academyId, String academyName) {
		Academy academy = Academy.restore(academyId, "kakao:" + academyId, academyName, "서울 강남구", OTHER_LOCATION, ENTRANCE, "02-000-0000");
		return PickupSchedule.restore(scheduleId, childUserId, academy, Set.of(DayOfWeek.TUESDAY), LocalTime.of(16, 0), 10,
				ScheduleStatus.READY);
	}
}

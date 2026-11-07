package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.ScheduleAcademy;
import com.example.pickupservice.schedule.application.port.in.ScheduleView;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ListSchedulesServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long OTHER_FAMILY_ID = 20L;
	private static final long CHILD_ID = 101L;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadSchedulePort loadSchedulePort;

	private ListSchedulesService listSchedulesService;

	@BeforeEach
	void setUp() {
		listSchedulesService = new ListSchedulesService(loadMemberUseCase, loadSchedulePort);
	}

	@Test
	@DisplayName("일정 목록은 자녀의 일정을 scheduleId · 학원 · weekdays · classEndTime · targetPickup · status로 돌려준다")
	void listSchedulesReturnsScheduleViewsOfChild() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		Academy english = Academy.restore(301L, "kakao:12345", "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, ENTRANCE,
				"02-555-1234");
		Academy math = Academy.restore(302L, "kakao:67890", "바른수학교실", "서울 강남구 역삼로 10", LOCATION, null, "02-555-5678");
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				PickupSchedule.restore(702L, CHILD_ID, math, Set.of(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY),
						LocalTime.of(17, 30), 10, ScheduleStatus.DRAFT),
				PickupSchedule.restore(701L, CHILD_ID, english,
						Set.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(18, 0), 5,
						ScheduleStatus.READY)));

		List<ScheduleView> views = listSchedulesService.listSchedules(new ChildQuery(FAMILY_ID, CHILD_ID));

		assertThat(views).containsExactly(
				new ScheduleView(701L, new ScheduleAcademy(301L, "해솔영어학원"),
						List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), LocalTime.of(18, 0), 5,
						ScheduleStatus.READY),
				new ScheduleView(702L, new ScheduleAcademy(302L, "바른수학교실"),
						List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), LocalTime.of(17, 30), 10,
						ScheduleStatus.DRAFT));
	}

	@Test
	@DisplayName("일정 목록은 일정이 없으면 빈 목록을 돌려준다")
	void listSchedulesReturnsEmptyListWhenChildHasNoSchedules() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of());

		List<ScheduleView> views = listSchedulesService.listSchedules(new ChildQuery(FAMILY_ID, CHILD_ID));

		assertThat(views).isEmpty();
	}

	@Test
	@DisplayName("일정 목록은 childId가 CHILD가 아니면 NOT_FOUND로 거절한다")
	void listSchedulesRejectsWithNotFoundWhenMemberIsNotChild() {
		givenMember(CHILD_ID, FAMILY_ID, Role.GUARDIAN);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> listSchedulesService.listSchedules(new ChildQuery(FAMILY_ID, CHILD_ID)));

		assertNotFound(thrown);
		verifyNoInteractions(loadSchedulePort);
	}

	@Test
	@DisplayName("일정 목록은 다른 가족의 자녀면 NOT_FOUND로 거절한다")
	void listSchedulesRejectsWithNotFoundWhenChildBelongsToOtherFamily() {
		givenMember(CHILD_ID, OTHER_FAMILY_ID, Role.CHILD);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> listSchedulesService.listSchedules(new ChildQuery(FAMILY_ID, CHILD_ID)));

		assertNotFound(thrown);
		verifyNoInteractions(loadSchedulePort);
	}

	@Test
	@DisplayName("일정 목록은 없는 childId면 NOT_FOUND로 거절한다")
	void listSchedulesRejectsWithNotFoundWhenMemberDoesNotExist() {
		given(loadMemberUseCase.findMember(CHILD_ID)).willReturn(Optional.empty());

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> listSchedulesService.listSchedules(new ChildQuery(FAMILY_ID, CHILD_ID)));

		assertNotFound(thrown);
		verifyNoInteractions(loadSchedulePort);
	}

	private void givenMember(long userId, long familyId, Role role) {
		given(loadMemberUseCase.findMember(userId))
				.willReturn(Optional.of(new MemberInfo(userId, familyId, role, "민준", "010-0000-0000")));
	}

	private static void assertNotFound(BusinessException thrown) {
		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(thrown.status()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}

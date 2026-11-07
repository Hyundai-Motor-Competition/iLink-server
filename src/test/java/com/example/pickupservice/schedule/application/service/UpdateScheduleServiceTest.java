package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleCommand;
import com.example.pickupservice.schedule.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SaveAcademyPort;
import com.example.pickupservice.schedule.application.port.out.SaveSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdateScheduleServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long OTHER_FAMILY_ID = 20L;
	private static final long CHILD_ID = 101L;
	private static final long OLD_ACADEMY_ID = 301L;
	private static final long NEW_ACADEMY_ID = 302L;
	private static final long EXISTING_NEW_PLACE_ACADEMY_ID = 303L;
	private static final String OLD_EXTERNAL_PLACE_ID = "kakao:12345";
	private static final String NEW_EXTERNAL_PLACE_ID = "kakao:67890";
	private static final long SCHEDULE_ID = 501L;

	private static final LatLng OLD_LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng OLD_ENTRANCE = new LatLng(37.501150, 127.039790);
	private static final LatLng NEW_LOCATION = new LatLng(37.497942, 127.027621);
	private static final LatLng NEW_ENTRANCE = new LatLng(37.497900, 127.027600);

	private static final Set<DayOfWeek> OLD_WEEKDAYS = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
	private static final LocalTime OLD_CLASS_END_TIME = LocalTime.of(18, 0);
	private static final int OLD_TARGET_PICKUP = 5;

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadSchedulePort loadSchedulePort;

	@Mock
	private LoadAcademyPort loadAcademyPort;

	@Mock
	private SaveAcademyPort saveAcademyPort;

	@Mock
	private SaveSchedulePort saveSchedulePort;

	private UpdateScheduleService updateScheduleService;

	@BeforeEach
	void setUp() {
		updateScheduleService = new UpdateScheduleService(loadMemberUseCase, loadSchedulePort, loadAcademyPort,
				saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 수정은 자기 자신과는 겹침으로 판정하지 않는다")
	void updateScheduleDoesNotTreatItselfAsOverlap() {
		PickupSchedule self = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME, oldAcademyWithEntrance(),
				ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(self));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(self));
		givenSavePortsAssignIds();

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, 10, oldAcademyInput()));

		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(scheduleCaptor.getValue().getTargetPickup()).isEqualTo(10);
	}

	@Test
	@DisplayName("일정 수정은 다른 일정과 겹치면 SCHEDULE_OVERLAP과 자기 자신을 뺀 겹치는 scheduleId 목록으로 거절한다")
	void updateScheduleRejectsWithScheduleOverlapExcludingItself() {
		PickupSchedule self = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME, oldAcademyWithEntrance(),
				ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(self));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(self,
				savedSchedule(801L, Set.of(DayOfWeek.MONDAY), OLD_CLASS_END_TIME, oldAcademyWithEntrance(),
						ScheduleStatus.READY),
				savedSchedule(803L, Set.of(DayOfWeek.MONDAY), LocalTime.of(17, 0), oldAcademyWithEntrance(),
						ScheduleStatus.READY)));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> updateScheduleService.updateSchedule(
						command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, 10, oldAcademyInput())));

		assertThat(thrown).isNotNull();
		assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(thrown.code()).isEqualTo("SCHEDULE_OVERLAP");
		assertThat(thrown.overlapScheduleIds()).containsExactly(801L);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 수정은 하루 한도를 셀 때 자기 자신을 빼고 센다")
	void updateScheduleExcludesItselfWhenCountingDailyLimit() {
		PickupSchedule self = savedSchedule(SCHEDULE_ID, Set.of(DayOfWeek.MONDAY), OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(self));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(self,
				savedSchedule(601L, Set.of(DayOfWeek.MONDAY), LocalTime.of(15, 0), oldAcademyWithEntrance(),
						ScheduleStatus.READY),
				savedSchedule(602L, Set.of(DayOfWeek.MONDAY), LocalTime.of(16, 0), oldAcademyWithEntrance(),
						ScheduleStatus.READY)));
		givenSavePortsAssignIds();

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(Set.of(DayOfWeek.MONDAY), LocalTime.of(19, 0), OLD_TARGET_PICKUP, oldAcademyInput()));

		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		verify(saveSchedulePort).saveSchedule(any(PickupSchedule.class));
	}

	@Test
	@DisplayName("일정 수정은 자기 자신을 뺀 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절하고 저장하지 않는다")
	void updateScheduleRejectsWithDailyScheduleLimitWhenOtherSchedulesOnSameWeekdayAreAlreadyThree() {
		PickupSchedule self = savedSchedule(SCHEDULE_ID, Set.of(DayOfWeek.TUESDAY), OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(self));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(self,
				savedSchedule(601L, Set.of(DayOfWeek.MONDAY), LocalTime.of(15, 0), oldAcademyWithEntrance(),
						ScheduleStatus.READY),
				savedSchedule(602L, Set.of(DayOfWeek.MONDAY), LocalTime.of(16, 0), oldAcademyWithEntrance(),
						ScheduleStatus.READY),
				savedSchedule(603L, Set.of(DayOfWeek.MONDAY), LocalTime.of(17, 0), oldAcademyWithEntrance(),
						ScheduleStatus.READY)));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> updateScheduleService.updateSchedule(
						command(Set.of(DayOfWeek.MONDAY), LocalTime.of(19, 0), OLD_TARGET_PICKUP, oldAcademyInput())));

		assertThat(thrown).as("월요일에 다른 일정 3개가 있을 때 월요일로 옮기는 수정").isNotNull();
		assertThat(thrown.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(thrown.code()).isEqualTo("DAILY_SCHEDULE_LIMIT");
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 수정은 같은 학원을 다시 쓸 때 요청의 entrance · phone이 비어 있으면 기존 entrance · phone을 유지해 저장하고 status는 READY다")
	void updateScheduleKeepsExistingEntranceAndPhoneWhenRequestLeavesThemEmpty() {
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(existing));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(existing));
		Academy existingAcademy = Academy.restore(EXISTING_NEW_PLACE_ACADEMY_ID, NEW_EXTERNAL_PLACE_ID, "새봄수학",
				"서울 서초구 옛 주소 3", OLD_LOCATION, NEW_ENTRANCE, "02-777-1111");
		given(loadAcademyPort.loadAcademyByExternalPlaceId(NEW_EXTERNAL_PLACE_ID))
				.willReturn(Optional.of(existingAcademy));
		givenSavePortsAssignIds();
		AcademyInput withoutEntranceAndPhone = new AcademyInput(NEW_EXTERNAL_PLACE_ID, "새봄수학학원",
				"서울 서초구 서초대로 77", NEW_LOCATION, null, null);

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, OLD_TARGET_PICKUP, withoutEntranceAndPhone));

		assertThat(result).as("수정 결과 (기존 출입구를 유지했으니 READY)")
				.isEqualTo(new ScheduleResult(SCHEDULE_ID, EXISTING_NEW_PLACE_ACADEMY_ID, ScheduleStatus.READY));

		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy savedAcademy = academyCaptor.getValue();
		assertThat(savedAcademy.getAcademyId()).as("saveAcademy에 넘긴 학원의 academyId")
				.isEqualTo(EXISTING_NEW_PLACE_ACADEMY_ID);
		assertThat(savedAcademy.getName()).as("요청 값으로 갱신한 name").isEqualTo("새봄수학학원");
		assertThat(savedAcademy.getAddress()).as("요청 값으로 갱신한 address").isEqualTo("서울 서초구 서초대로 77");
		assertThat(savedAcademy.getLocation()).as("요청 값으로 갱신한 location").isEqualTo(NEW_LOCATION);
		assertThat(savedAcademy.getEntrance()).as("요청 entrance가 비어 있어 유지한 기존 entrance").isEqualTo(NEW_ENTRANCE);
		assertThat(savedAcademy.getPhone()).as("요청 phone이 비어 있어 유지한 기존 phone").isEqualTo("02-777-1111");

		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getStatus()).as("저장한 일정의 status").isEqualTo(ScheduleStatus.READY);
		assertThat(scheduleCaptor.getValue().getAcademy().getEntrance()).as("저장한 일정의 학원 entrance")
				.isEqualTo(NEW_ENTRANCE);
	}

	@Test
	@DisplayName("일정 수정은 요일 · 시간 · 학원을 바꾸고 status를 다시 판정한다 (출입구 있는 학원 → 없는 학원이면 DRAFT)")
	void updateScheduleChangesWeekdaysTimeAcademyAndRejudgesStatusToDraft() {
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(existing));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(existing));
		givenSavePortsAssignIds();
		Set<DayOfWeek> newWeekdays = Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
		LocalTime newClassEndTime = LocalTime.of(17, 30);

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(newWeekdays, newClassEndTime, 10, newAcademyInput(null)));

		assertThat(result).isEqualTo(new ScheduleResult(SCHEDULE_ID, NEW_ACADEMY_ID, ScheduleStatus.DRAFT));

		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy newAcademy = academyCaptor.getValue();
		assertThat(newAcademy.getAcademyId()).isNull();
		assertThat(newAcademy.getName()).isEqualTo("새봄수학학원");
		assertThat(newAcademy.getAddress()).isEqualTo("서울 서초구 서초대로 77");
		assertThat(newAcademy.getLocation()).isEqualTo(NEW_LOCATION);
		assertThat(newAcademy.getEntrance()).isNull();
		assertThat(newAcademy.getPhone()).isEqualTo("02-777-0000");

		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		PickupSchedule saved = scheduleCaptor.getValue();
		assertThat(saved.getScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(saved.getChildUserId()).isEqualTo(CHILD_ID);
		assertThat(saved.getAcademy().getAcademyId()).isEqualTo(NEW_ACADEMY_ID);
		assertThat(saved.getWeekdays()).containsExactlyInAnyOrderElementsOf(newWeekdays);
		assertThat(saved.getClassEndTime()).isEqualTo(newClassEndTime);
		assertThat(saved.getTargetPickup()).isEqualTo(10);
		assertThat(saved.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
	}

	@Test
	@DisplayName("일정 수정은 요일 · 시간 · 학원을 바꾸고 status를 다시 판정한다 (출입구 없는 학원 → 있는 학원이면 READY)")
	void updateScheduleRejudgesStatusToReadyWhenNewAcademyHasEntrance() {
		Academy oldAcademyWithoutEntrance = Academy.restore(OLD_ACADEMY_ID, OLD_EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152",
				OLD_LOCATION, null, "02-555-1234");
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithoutEntrance, ScheduleStatus.DRAFT);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(existing));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(existing));
		givenSavePortsAssignIds();

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(Set.of(DayOfWeek.FRIDAY), LocalTime.of(16, 0), OLD_TARGET_PICKUP,
						newAcademyInput(NEW_ENTRANCE)));

		assertThat(result).isEqualTo(new ScheduleResult(SCHEDULE_ID, NEW_ACADEMY_ID, ScheduleStatus.READY));
		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(scheduleCaptor.getValue().getStatus()).isEqualTo(ScheduleStatus.READY);
		assertThat(scheduleCaptor.getValue().getAcademy().getEntrance()).isEqualTo(NEW_ENTRANCE);
	}

	@Test
	@DisplayName("일정 수정은 바꾼 학원과 같은 externalPlaceId의 학원이 있으면 새로 만들지 않고 그 academyId로 저장하며 학원 정보는 요청 값으로 갱신하고 그 academyId를 돌려준다")
	void updateScheduleReusesExistingAcademyWithSameExternalPlaceIdAndUpdatesItsInfo() {
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(existing));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(existing));
		Academy existingAcademy = Academy.restore(EXISTING_NEW_PLACE_ACADEMY_ID, NEW_EXTERNAL_PLACE_ID, "새봄수학",
				"서울 서초구 옛 주소 3", OLD_LOCATION, null, null);
		given(loadAcademyPort.loadAcademyByExternalPlaceId(NEW_EXTERNAL_PLACE_ID)).willReturn(Optional.of(existingAcademy));
		givenSavePortsAssignIds();

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, OLD_TARGET_PICKUP, newAcademyInput(NEW_ENTRANCE)));

		assertThat(result).as("수정 결과")
				.isEqualTo(new ScheduleResult(SCHEDULE_ID, EXISTING_NEW_PLACE_ACADEMY_ID, ScheduleStatus.READY));

		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy savedAcademy = academyCaptor.getValue();
		assertThat(savedAcademy.getAcademyId()).as("saveAcademy에 넘긴 학원의 academyId")
				.isEqualTo(EXISTING_NEW_PLACE_ACADEMY_ID);
		assertThat(savedAcademy.getExternalPlaceId()).isEqualTo(NEW_EXTERNAL_PLACE_ID);
		assertThat(savedAcademy.getName()).as("요청 값으로 갱신한 name").isEqualTo("새봄수학학원");
		assertThat(savedAcademy.getAddress()).as("요청 값으로 갱신한 address").isEqualTo("서울 서초구 서초대로 77");
		assertThat(savedAcademy.getLocation()).as("요청 값으로 갱신한 location").isEqualTo(NEW_LOCATION);
		assertThat(savedAcademy.getEntrance()).as("요청 값으로 갱신한 entrance").isEqualTo(NEW_ENTRANCE);
		assertThat(savedAcademy.getPhone()).as("요청 값으로 갱신한 phone").isEqualTo("02-777-0000");

		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getScheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(scheduleCaptor.getValue().getAcademy().getAcademyId()).as("저장한 일정의 academyId")
				.isEqualTo(EXISTING_NEW_PLACE_ACADEMY_ID);
	}

	@Test
	@DisplayName("일정 수정은 바꾼 학원과 같은 externalPlaceId의 학원이 없으면 academyId 없는 새 학원을 externalPlaceId와 함께 저장한다")
	void updateScheduleCreatesNewAcademyWhenNoAcademyHasSameExternalPlaceId() {
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(existing));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(existing));
		given(loadAcademyPort.loadAcademyByExternalPlaceId(NEW_EXTERNAL_PLACE_ID)).willReturn(Optional.empty());
		givenSavePortsAssignIds();

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, OLD_TARGET_PICKUP, newAcademyInput(NEW_ENTRANCE)));

		verify(loadAcademyPort).loadAcademyByExternalPlaceId(NEW_EXTERNAL_PLACE_ID);
		assertThat(result).isEqualTo(new ScheduleResult(SCHEDULE_ID, NEW_ACADEMY_ID, ScheduleStatus.READY));
		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy newAcademy = academyCaptor.getValue();
		assertThat(newAcademy.getAcademyId()).as("새 학원의 academyId").isNull();
		assertThat(newAcademy.getExternalPlaceId()).isEqualTo(NEW_EXTERNAL_PLACE_ID);
		assertThat(newAcademy.getName()).isEqualTo("새봄수학학원");
	}

	@Test
	@DisplayName("일정 수정은 학원이 그대로(같은 externalPlaceId)면 academyId를 유지하고 학원 정보는 요청 값으로 갱신한다")
	void updateScheduleKeepsAcademyIdWhenExternalPlaceIdIsUnchanged() {
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(existing));
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(existing));
		Academy sameAcademy = oldAcademyWithEntrance();
		lenient().when(loadAcademyPort.loadAcademyByExternalPlaceId(OLD_EXTERNAL_PLACE_ID))
				.thenReturn(Optional.of(sameAcademy));
		givenSavePortsAssignIds();
		AcademyInput samePlaceNewPhone = new AcademyInput(OLD_EXTERNAL_PLACE_ID, "해솔영어학원",
				"서울 강남구 테헤란로 152", OLD_LOCATION, OLD_ENTRANCE, "02-555-9999");

		ScheduleResult result = updateScheduleService.updateSchedule(
				command(OLD_WEEKDAYS, LocalTime.of(19, 0), OLD_TARGET_PICKUP, samePlaceNewPhone));

		assertThat(result).as("수정 결과")
				.isEqualTo(new ScheduleResult(SCHEDULE_ID, OLD_ACADEMY_ID, ScheduleStatus.READY));
		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		assertThat(academyCaptor.getValue().getAcademyId()).as("saveAcademy에 넘긴 학원의 academyId")
				.isEqualTo(OLD_ACADEMY_ID);
		assertThat(academyCaptor.getValue().getExternalPlaceId()).isEqualTo(OLD_EXTERNAL_PLACE_ID);
		assertThat(academyCaptor.getValue().getPhone()).as("요청 값으로 갱신한 phone").isEqualTo("02-555-9999");
		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getAcademy().getAcademyId()).as("저장한 일정의 academyId")
				.isEqualTo(OLD_ACADEMY_ID);
		assertThat(scheduleCaptor.getValue().getClassEndTime()).isEqualTo(LocalTime.of(19, 0));
	}

	@Test
	@DisplayName("일정 수정은 없는 scheduleId면 NOT_FOUND로 거절한다")
	void updateScheduleRejectsWithNotFoundWhenScheduleDoesNotExist() {
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.empty());

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> updateScheduleService.updateSchedule(
						command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, OLD_TARGET_PICKUP, oldAcademyInput())));

		assertNotFound(thrown);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 수정은 다른 가족 자녀의 일정이면 NOT_FOUND로 거절한다")
	void updateScheduleRejectsWithNotFoundWhenScheduleBelongsToOtherFamilyChild() {
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedule(SCHEDULE_ID)).willReturn(Optional.of(existing));
		givenMember(CHILD_ID, OTHER_FAMILY_ID, Role.CHILD);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> updateScheduleService.updateSchedule(
						command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, OLD_TARGET_PICKUP, oldAcademyInput())));

		assertNotFound(thrown);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 수정은 targetPickup이 0~60을 벗어나면 VALIDATION_FAILED로 거절한다")
	void updateScheduleRejectsWhenTargetPickupIsOutOfRange() {
		givenExistingScheduleOfMyChildLeniently();

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> updateScheduleService.updateSchedule(
						command(OLD_WEEKDAYS, OLD_CLASS_END_TIME, 61, oldAcademyInput())));

		assertValidationFailedOn(thrown, "targetPickup");
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 수정은 weekdays가 비어 있으면 VALIDATION_FAILED로 거절한다")
	void updateScheduleRejectsWhenWeekdaysIsEmpty() {
		givenExistingScheduleOfMyChildLeniently();

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> updateScheduleService.updateSchedule(
						command(Set.of(), OLD_CLASS_END_TIME, OLD_TARGET_PICKUP, oldAcademyInput())));

		assertValidationFailedOn(thrown, "weekdays");
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	private void givenExistingScheduleOfMyChildLeniently() {
		PickupSchedule existing = savedSchedule(SCHEDULE_ID, OLD_WEEKDAYS, OLD_CLASS_END_TIME,
				oldAcademyWithEntrance(), ScheduleStatus.READY);
		lenient().when(loadSchedulePort.loadSchedule(SCHEDULE_ID)).thenReturn(Optional.of(existing));
		lenient().when(loadMemberUseCase.findMember(CHILD_ID))
				.thenReturn(Optional.of(new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", "010-0000-0000")));
		lenient().when(loadSchedulePort.loadSchedulesOfChild(anyLong())).thenReturn(List.of(existing));
	}

	private void givenMember(long userId, long familyId, Role role) {
		given(loadMemberUseCase.findMember(userId))
				.willReturn(Optional.of(new MemberInfo(userId, familyId, role, "민준", "010-0000-0000")));
	}

	private void givenSavePortsAssignIds() {
		willAnswer(invocation -> {
			Academy academy = invocation.getArgument(0);
			if (academy.getAcademyId() != null) {
				return academy;
			}
			return Academy.restore(NEW_ACADEMY_ID, academy.getExternalPlaceId(), academy.getName(),
					academy.getAddress(), academy.getLocation(), academy.getEntrance(), academy.getPhone());
		}).given(saveAcademyPort).saveAcademy(any(Academy.class));
		willAnswer(invocation -> invocation.getArgument(0))
				.given(saveSchedulePort).saveSchedule(any(PickupSchedule.class));
	}

	private static Academy oldAcademyWithEntrance() {
		return Academy.restore(OLD_ACADEMY_ID, OLD_EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", OLD_LOCATION,
				OLD_ENTRANCE, "02-555-1234");
	}

	private static PickupSchedule savedSchedule(long scheduleId, Set<DayOfWeek> weekdays, LocalTime classEndTime,
			Academy academy, ScheduleStatus status) {
		return PickupSchedule.restore(scheduleId, CHILD_ID, academy, weekdays, classEndTime, OLD_TARGET_PICKUP,
				status);
	}

	private static AcademyInput oldAcademyInput() {
		return new AcademyInput(OLD_EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", OLD_LOCATION, OLD_ENTRANCE,
				"02-555-1234");
	}

	private static AcademyInput newAcademyInput(LatLng entrance) {
		return new AcademyInput(NEW_EXTERNAL_PLACE_ID, "새봄수학학원", "서울 서초구 서초대로 77", NEW_LOCATION, entrance,
				"02-777-0000");
	}

	private static UpdateScheduleCommand command(Set<DayOfWeek> weekdays, LocalTime classEndTime, int targetPickup,
			AcademyInput academy) {
		return new UpdateScheduleCommand(FAMILY_ID, SCHEDULE_ID, academy, weekdays, classEndTime, targetPickup);
	}

	private static void assertNotFound(BusinessException thrown) {
		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(thrown.status()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private static void assertValidationFailedOn(BusinessException thrown, String field) {
		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(thrown.fields()).extracting("field").containsExactly(field);
	}
}

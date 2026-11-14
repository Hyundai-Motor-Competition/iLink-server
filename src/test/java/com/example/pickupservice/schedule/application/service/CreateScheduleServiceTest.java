package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateScheduleServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long OTHER_FAMILY_ID = 20L;
	private static final long CHILD_ID = 101L;
	private static final long SIBLING_ID = 102L;
	private static final long SAVED_ACADEMY_ID = 301L;
	private static final long EXISTING_ACADEMY_ID = 401L;
	private static final String EXTERNAL_PLACE_ID = "kakao:12345";
	private static final long SAVED_SCHEDULE_ID = 501L;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);
	private static final LatLng OLD_LOCATION = new LatLng(37.500000, 127.030000);
	private static final Set<DayOfWeek> WEEKDAYS = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
	private static final LocalTime CLASS_END_TIME = LocalTime.of(18, 0);
	private static final int TARGET_PICKUP = 5;

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

	private CreateScheduleService createScheduleService;

	@BeforeEach
	void setUp() {
		createScheduleService = new CreateScheduleService(loadMemberUseCase, loadSchedulePort, loadAcademyPort,
				saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 학원을 만들고 scheduleId · academyId · status를 돌려준다")
	void createScheduleSavesAcademyAndScheduleAndReturnsIds() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		givenSavePortsAssignIds();

		ScheduleResult result = createScheduleService.createSchedule(command(ENTRANCE));

		assertThat(result).isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, SAVED_ACADEMY_ID, ScheduleStatus.READY));

		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy newAcademy = academyCaptor.getValue();
		assertThat(newAcademy.getAcademyId()).isNull();
		assertThat(newAcademy.getName()).isEqualTo("해솔영어학원");
		assertThat(newAcademy.getAddress()).isEqualTo("서울 강남구 테헤란로 152");
		assertThat(newAcademy.getLocation()).isEqualTo(LOCATION);
		assertThat(newAcademy.getEntrance()).isEqualTo(ENTRANCE);
		assertThat(newAcademy.getPhone()).isEqualTo("02-555-1234");

		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		PickupSchedule newSchedule = scheduleCaptor.getValue();
		assertThat(newSchedule.getScheduleId()).isNull();
		assertThat(newSchedule.getChildUserId()).isEqualTo(CHILD_ID);
		assertThat(newSchedule.getAcademy().getAcademyId()).isEqualTo(SAVED_ACADEMY_ID);
		assertThat(newSchedule.getWeekdays()).containsExactlyInAnyOrderElementsOf(WEEKDAYS);
		assertThat(newSchedule.getClassEndTime()).isEqualTo(CLASS_END_TIME);
		assertThat(newSchedule.getTargetPickup()).isEqualTo(TARGET_PICKUP);
		assertThat(newSchedule.getStatus()).isEqualTo(ScheduleStatus.READY);
	}

	@Test
	@DisplayName("일정 등록은 출입구 없는 학원이면 status DRAFT를 돌려준다")
	void createScheduleReturnsDraftWhenAcademyHasNoEntrance() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		givenSavePortsAssignIds();

		ScheduleResult result = createScheduleService.createSchedule(command(null));

		assertThat(result).isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, SAVED_ACADEMY_ID, ScheduleStatus.DRAFT));
		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getStatus()).isEqualTo(ScheduleStatus.DRAFT);
		assertThat(scheduleCaptor.getValue().getAcademy().getEntrance()).isNull();
	}

	@Test
	@DisplayName("일정 등록은 같은 externalPlaceId의 학원이 있으면 새로 만들지 않고 그 academyId로 저장하며 학원 정보는 요청 값으로 갱신하고 그 academyId를 돌려준다")
	void createScheduleReusesExistingAcademyWithSameExternalPlaceIdAndUpdatesItsInfo() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		Academy existingAcademy = Academy.restore(EXISTING_ACADEMY_ID, FAMILY_ID, EXTERNAL_PLACE_ID, "해솔영어", "서울 강남구 옛 주소 1",
				OLD_LOCATION, null, null);
		given(loadAcademyPort.loadAcademyByExternalPlaceId(FAMILY_ID, EXTERNAL_PLACE_ID)).willReturn(Optional.of(existingAcademy));
		givenSavePortsAssignIds();

		ScheduleResult result = createScheduleService.createSchedule(command(ENTRANCE));

		assertThat(result).as("등록 결과")
				.isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, EXISTING_ACADEMY_ID, ScheduleStatus.READY));

		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy savedAcademy = academyCaptor.getValue();
		assertThat(savedAcademy.getAcademyId()).as("saveAcademy에 넘긴 학원의 academyId").isEqualTo(EXISTING_ACADEMY_ID);
		assertThat(savedAcademy.getExternalPlaceId()).isEqualTo(EXTERNAL_PLACE_ID);
		assertThat(savedAcademy.getName()).as("요청 값으로 갱신한 name").isEqualTo("해솔영어학원");
		assertThat(savedAcademy.getAddress()).as("요청 값으로 갱신한 address").isEqualTo("서울 강남구 테헤란로 152");
		assertThat(savedAcademy.getLocation()).as("요청 값으로 갱신한 location").isEqualTo(LOCATION);
		assertThat(savedAcademy.getEntrance()).as("요청 값으로 갱신한 entrance").isEqualTo(ENTRANCE);
		assertThat(savedAcademy.getPhone()).as("요청 값으로 갱신한 phone").isEqualTo("02-555-1234");

		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getAcademy().getAcademyId()).as("저장한 일정의 academyId")
				.isEqualTo(EXISTING_ACADEMY_ID);
		assertThat(scheduleCaptor.getValue().getStatus()).isEqualTo(ScheduleStatus.READY);
	}

	@Test
	@DisplayName("일정 등록은 같은 학원을 다시 쓸 때 요청의 entrance · phone이 비어 있으면 기존 entrance · phone을 유지해 저장하고 status는 READY다")
	void createScheduleKeepsExistingEntranceAndPhoneWhenRequestLeavesThemEmpty() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		Academy existingAcademy = Academy.restore(EXISTING_ACADEMY_ID, FAMILY_ID, EXTERNAL_PLACE_ID, "해솔영어", "서울 강남구 옛 주소 1",
				OLD_LOCATION, ENTRANCE, "02-555-0000");
		given(loadAcademyPort.loadAcademyByExternalPlaceId(FAMILY_ID, EXTERNAL_PLACE_ID)).willReturn(Optional.of(existingAcademy));
		givenSavePortsAssignIds();
		AcademyInput withoutEntranceAndPhone = new AcademyInput(EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152",
				LOCATION, null, null);

		ScheduleResult result = createScheduleService.createSchedule(
				new CreateScheduleCommand(FAMILY_ID, CHILD_ID, withoutEntranceAndPhone, WEEKDAYS, CLASS_END_TIME,
						TARGET_PICKUP));

		assertThat(result).as("등록 결과 (기존 출입구를 유지했으니 READY)")
				.isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, EXISTING_ACADEMY_ID, ScheduleStatus.READY));

		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy savedAcademy = academyCaptor.getValue();
		assertThat(savedAcademy.getAcademyId()).as("saveAcademy에 넘긴 학원의 academyId").isEqualTo(EXISTING_ACADEMY_ID);
		assertThat(savedAcademy.getName()).as("요청 값으로 갱신한 name").isEqualTo("해솔영어학원");
		assertThat(savedAcademy.getAddress()).as("요청 값으로 갱신한 address").isEqualTo("서울 강남구 테헤란로 152");
		assertThat(savedAcademy.getLocation()).as("요청 값으로 갱신한 location").isEqualTo(LOCATION);
		assertThat(savedAcademy.getEntrance()).as("요청 entrance가 비어 있어 유지한 기존 entrance").isEqualTo(ENTRANCE);
		assertThat(savedAcademy.getPhone()).as("요청 phone이 비어 있어 유지한 기존 phone").isEqualTo("02-555-0000");

		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getStatus()).as("저장한 일정의 status").isEqualTo(ScheduleStatus.READY);
		assertThat(scheduleCaptor.getValue().getAcademy().getEntrance()).as("저장한 일정의 학원 entrance")
				.isEqualTo(ENTRANCE);
	}

	@Test
	@DisplayName("일정 등록은 같은 externalPlaceId의 학원이 없으면 academyId 없는 새 학원을 externalPlaceId와 함께 저장한다")
	void createScheduleCreatesNewAcademyWhenNoAcademyHasSameExternalPlaceId() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadAcademyPort.loadAcademyByExternalPlaceId(FAMILY_ID, EXTERNAL_PLACE_ID)).willReturn(Optional.empty());
		givenSavePortsAssignIds();

		ScheduleResult result = createScheduleService.createSchedule(command(ENTRANCE));

		verify(loadAcademyPort).loadAcademyByExternalPlaceId(FAMILY_ID, EXTERNAL_PLACE_ID);
		assertThat(result).isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, SAVED_ACADEMY_ID, ScheduleStatus.READY));
		ArgumentCaptor<Academy> academyCaptor = ArgumentCaptor.forClass(Academy.class);
		verify(saveAcademyPort).saveAcademy(academyCaptor.capture());
		Academy newAcademy = academyCaptor.getValue();
		assertThat(newAcademy.getAcademyId()).as("새 학원의 academyId").isNull();
		assertThat(newAcademy.getFamilyId()).as("새 학원의 familyId (자녀의 가족)").isEqualTo(FAMILY_ID);
		assertThat(newAcademy.getExternalPlaceId()).isEqualTo(EXTERNAL_PLACE_ID);
		assertThat(newAcademy.getName()).isEqualTo("해솔영어학원");
		assertThat(newAcademy.getAddress()).isEqualTo("서울 강남구 테헤란로 152");
		assertThat(newAcademy.getLocation()).isEqualTo(LOCATION);
		assertThat(newAcademy.getEntrance()).isEqualTo(ENTRANCE);
		assertThat(newAcademy.getPhone()).isEqualTo("02-555-1234");
	}

	@Test
	@DisplayName("일정 등록은 저장한 학원 기준으로 status를 판정한다 (같은 가족의 동시 등록이 먼저 출입구와 함께 저장한 새 학원이면 요청 entrance가 없어도 READY)")
	void createScheduleJudgesStatusBySavedAcademyKeepingEntranceSavedFirstByConcurrentRequest() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadAcademyPort.loadAcademyByExternalPlaceId(FAMILY_ID, EXTERNAL_PLACE_ID)).willReturn(Optional.empty());
		given(saveAcademyPort.saveAcademy(any(Academy.class))).willReturn(Academy.restore(EXISTING_ACADEMY_ID,
				FAMILY_ID, EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, ENTRANCE, "02-555-1234"));
		willAnswer(invocation -> {
			PickupSchedule schedule = invocation.getArgument(0);
			return PickupSchedule.restore(SAVED_SCHEDULE_ID, schedule.getChildUserId(), schedule.getAcademy(),
					schedule.getWeekdays(), schedule.getClassEndTime(), schedule.getTargetPickup(),
					schedule.getStatus());
		}).given(saveSchedulePort).saveSchedule(any(PickupSchedule.class));

		ScheduleResult result = createScheduleService.createSchedule(command(null));

		assertThat(result).as("등록 결과 (upsert가 지킨 기존 출입구 기준이라 READY)")
				.isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, EXISTING_ACADEMY_ID, ScheduleStatus.READY));
		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getStatus()).as("saveSchedule에 넘긴 일정의 status")
				.isEqualTo(ScheduleStatus.READY);
		assertThat(scheduleCaptor.getValue().getAcademy().getEntrance()).as("saveSchedule에 넘긴 일정의 학원 entrance")
				.isEqualTo(ENTRANCE);
	}

	@Test
	@DisplayName("일정 등록은 childId가 CHILD가 아니면 NOT_FOUND로 거절한다")
	void createScheduleRejectsWithNotFoundWhenMemberIsNotChild() {
		givenMember(CHILD_ID, FAMILY_ID, Role.GUARDIAN);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE)));

		assertNotFound(thrown);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 다른 가족의 자녀면 NOT_FOUND로 거절한다")
	void createScheduleRejectsWithNotFoundWhenChildBelongsToOtherFamily() {
		givenMember(CHILD_ID, OTHER_FAMILY_ID, Role.CHILD);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE)));

		assertNotFound(thrown);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 없는 childId면 NOT_FOUND로 거절한다")
	void createScheduleRejectsWithNotFoundWhenMemberDoesNotExist() {
		given(loadMemberUseCase.findMember(CHILD_ID)).willReturn(Optional.empty());

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE)));

		assertNotFound(thrown);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 해제된 자녀(removed true)면 NOT_FOUND로 거절하고 학원 · 일정을 저장하지 않는다")
	void createScheduleRejectsWithNotFoundWhenChildIsRemoved() {
		given(loadMemberUseCase.findMember(CHILD_ID)).willReturn(Optional.of(
				new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", "010-0000-0000", null, true)));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE)));

		assertNotFound(thrown);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록으로 학원에 출입구가 생기면 같은 가족이 그 학원을 쓰는 다른 DRAFT 일정을 READY로 다시 저장하고 이미 READY인 일정은 다시 저장하지 않는다")
	void createScheduleRejudgesOtherDraftSchedulesOfSameAcademyToReadyWhenEntranceIsAdded() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		Academy academyWithoutEntrance = Academy.restore(EXISTING_ACADEMY_ID, FAMILY_ID, EXTERNAL_PLACE_ID, "해솔영어학원",
				"서울 강남구 테헤란로 152", LOCATION, null, "02-555-1234");
		given(loadAcademyPort.loadAcademyByExternalPlaceId(FAMILY_ID, EXTERNAL_PLACE_ID))
				.willReturn(Optional.of(academyWithoutEntrance));
		PickupSchedule siblingDraft = PickupSchedule.restore(701L, SIBLING_ID, academyWithoutEntrance,
				Set.of(DayOfWeek.TUESDAY), LocalTime.of(17, 0), 10, ScheduleStatus.DRAFT);
		PickupSchedule siblingReady = PickupSchedule.restore(702L, SIBLING_ID, academyWithoutEntrance,
				Set.of(DayOfWeek.THURSDAY), LocalTime.of(17, 0), 10, ScheduleStatus.READY);
		given(loadSchedulePort.loadSchedulesOfAcademy(EXISTING_ACADEMY_ID))
				.willReturn(List.of(siblingDraft, siblingReady));
		givenSavePortsAssignIds();

		ScheduleResult result = createScheduleService.createSchedule(command(ENTRANCE));

		assertThat(result).as("등록 결과")
				.isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, EXISTING_ACADEMY_ID, ScheduleStatus.READY));
		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort, times(2)).saveSchedule(scheduleCaptor.capture());
		List<PickupSchedule> savedSchedules = scheduleCaptor.getAllValues();
		assertThat(savedSchedules).extracting(PickupSchedule::getScheduleId)
				.as("저장한 일정 (새 일정 + 다른 DRAFT 일정 701, 이미 READY인 702는 빠짐)")
				.containsExactlyInAnyOrder(null, 701L);
		PickupSchedule resaved = savedSchedules.stream()
				.filter(schedule -> Long.valueOf(701L).equals(schedule.getScheduleId()))
				.findFirst()
				.orElseThrow();
		assertThat(resaved.getStatus()).as("다시 저장한 701의 status").isEqualTo(ScheduleStatus.READY);
		assertThat(resaved.getChildUserId()).as("701의 자녀 (그대로)").isEqualTo(SIBLING_ID);
		assertThat(resaved.getWeekdays()).as("701의 weekdays (그대로)").containsExactly(DayOfWeek.TUESDAY);
		assertThat(resaved.getClassEndTime()).as("701의 classEndTime (그대로)").isEqualTo(LocalTime.of(17, 0));
		assertThat(resaved.getTargetPickup()).as("701의 targetPickup (그대로)").isEqualTo(10);
		assertThat(resaved.getAcademy().getAcademyId()).as("701의 academyId").isEqualTo(EXISTING_ACADEMY_ID);
		assertThat(resaved.getAcademy().getEntrance()).as("701의 학원 entrance (저장한 학원)").isEqualTo(ENTRANCE);
	}

	@Test
	@DisplayName("일정 등록으로 저장한 학원에 출입구가 없으면 같은 학원을 쓰는 다른 DRAFT 일정은 다시 저장하지 않는다")
	void createScheduleLeavesOtherDraftSchedulesOfSameAcademyWhenAcademyHasNoEntrance() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		Academy academyWithoutEntrance = Academy.restore(EXISTING_ACADEMY_ID, FAMILY_ID, EXTERNAL_PLACE_ID, "해솔영어학원",
				"서울 강남구 테헤란로 152", LOCATION, null, "02-555-1234");
		given(loadAcademyPort.loadAcademyByExternalPlaceId(FAMILY_ID, EXTERNAL_PLACE_ID))
				.willReturn(Optional.of(academyWithoutEntrance));
		PickupSchedule siblingDraft = PickupSchedule.restore(701L, SIBLING_ID, academyWithoutEntrance,
				Set.of(DayOfWeek.TUESDAY), LocalTime.of(17, 0), 10, ScheduleStatus.DRAFT);
		lenient().when(loadSchedulePort.loadSchedulesOfAcademy(EXISTING_ACADEMY_ID))
				.thenReturn(List.of(siblingDraft));
		givenSavePortsAssignIds();

		ScheduleResult result = createScheduleService.createSchedule(command(null));

		assertThat(result).as("등록 결과")
				.isEqualTo(new ScheduleResult(SAVED_SCHEDULE_ID, EXISTING_ACADEMY_ID, ScheduleStatus.DRAFT));
		ArgumentCaptor<PickupSchedule> scheduleCaptor = ArgumentCaptor.forClass(PickupSchedule.class);
		verify(saveSchedulePort, times(1)).saveSchedule(scheduleCaptor.capture());
		assertThat(scheduleCaptor.getValue().getScheduleId()).as("저장한 일정은 새 일정 하나뿐").isNull();
		assertThat(scheduleCaptor.getValue().getStatus()).isEqualTo(ScheduleStatus.DRAFT);
	}

	@Test
	@DisplayName("일정 등록은 같은 요일 일정이 이미 3개면 DAILY_SCHEDULE_LIMIT으로 거절한다")
	void createScheduleRejectsWithDailyScheduleLimitWhenSameWeekdayAlreadyHasThreeSchedules() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				existingSchedule(601L, Set.of(DayOfWeek.MONDAY), LocalTime.of(15, 0)),
				existingSchedule(602L, Set.of(DayOfWeek.MONDAY), LocalTime.of(16, 0)),
				existingSchedule(603L, Set.of(DayOfWeek.MONDAY), LocalTime.of(17, 0))));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE)));

		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo("DAILY_SCHEDULE_LIMIT");
		assertThat(thrown.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 겹치는 일정이 있으면 SCHEDULE_OVERLAP과 겹치는 scheduleId 목록으로 거절한다")
	void createScheduleRejectsWithScheduleOverlapAndOverlappingScheduleIds() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				existingSchedule(801L, Set.of(DayOfWeek.MONDAY), CLASS_END_TIME),
				existingSchedule(802L, Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), CLASS_END_TIME),
				existingSchedule(803L, Set.of(DayOfWeek.MONDAY), LocalTime.of(17, 0))));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE)));

		assertThat(thrown).isNotNull();
		assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(thrown.code()).isEqualTo("SCHEDULE_OVERLAP");
		assertThat(thrown.overlapScheduleIds()).containsExactlyInAnyOrder(801L, 802L);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 겹침과 하루 한도가 둘 다 해당하면 SCHEDULE_OVERLAP으로 먼저 거절한다")
	void createScheduleRejectsWithScheduleOverlapBeforeDailyScheduleLimit() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				existingSchedule(601L, Set.of(DayOfWeek.MONDAY), LocalTime.of(15, 0)),
				existingSchedule(602L, Set.of(DayOfWeek.MONDAY), LocalTime.of(16, 0)),
				existingSchedule(801L, Set.of(DayOfWeek.MONDAY), CLASS_END_TIME)));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE)));

		assertThat(thrown).isNotNull();
		assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(thrown.code()).isEqualTo("SCHEDULE_OVERLAP");
		assertThat(thrown.overlapScheduleIds()).containsExactly(801L);
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 targetPickup 61이면 도메인 예외를 422 VALIDATION_FAILED(fields[targetPickup])로 바꿔 거절한다")
	void createScheduleRejectsWithValidationFailedWhenTargetPickupIsOutOfRange() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE, WEEKDAYS, 61)));

		assertValidationFailedOn(thrown, new FieldError("targetPickup", "0~60이어야 합니다"));
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	@Test
	@DisplayName("일정 등록은 weekdays가 비어 있으면 도메인 예외를 422 VALIDATION_FAILED(fields[weekdays])로 바꿔 거절한다")
	void createScheduleRejectsWithValidationFailedWhenWeekdaysIsEmpty() {
		givenMember(CHILD_ID, FAMILY_ID, Role.CHILD);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> createScheduleService.createSchedule(command(ENTRANCE, Set.of(), TARGET_PICKUP)));

		assertValidationFailedOn(thrown, new FieldError("weekdays", "1개 이상이어야 합니다"));
		verifyNoInteractions(saveAcademyPort, saveSchedulePort);
	}

	private static PickupSchedule existingSchedule(long scheduleId, Set<DayOfWeek> weekdays, LocalTime classEndTime) {
		Academy academy = Academy.restore(SAVED_ACADEMY_ID, FAMILY_ID, EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, ENTRANCE,
				"02-555-1234");
		return PickupSchedule.restore(scheduleId, CHILD_ID, academy, weekdays, classEndTime, TARGET_PICKUP,
				ScheduleStatus.READY);
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
			return Academy.restore(SAVED_ACADEMY_ID, academy.getFamilyId(), academy.getExternalPlaceId(), academy.getName(),
					academy.getAddress(), academy.getLocation(), academy.getEntrance(), academy.getPhone());
		}).given(saveAcademyPort).saveAcademy(any(Academy.class));
		willAnswer(invocation -> {
			PickupSchedule schedule = invocation.getArgument(0);
			if (schedule.getScheduleId() != null) {
				return schedule;
			}
			return PickupSchedule.restore(SAVED_SCHEDULE_ID, schedule.getChildUserId(), schedule.getAcademy(),
					schedule.getWeekdays(), schedule.getClassEndTime(), schedule.getTargetPickup(),
					schedule.getStatus());
		}).given(saveSchedulePort).saveSchedule(any(PickupSchedule.class));
	}

	private static CreateScheduleCommand command(LatLng entrance) {
		return command(entrance, WEEKDAYS, TARGET_PICKUP);
	}

	private static CreateScheduleCommand command(LatLng entrance, Set<DayOfWeek> weekdays, int targetPickup) {
		AcademyInput academy = new AcademyInput(EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, entrance,
				"02-555-1234");
		return new CreateScheduleCommand(FAMILY_ID, CHILD_ID, academy, weekdays, CLASS_END_TIME, targetPickup);
	}

	private static void assertValidationFailedOn(BusinessException thrown, FieldError expected) {
		assertThat(thrown).isNotNull();
		assertThat(thrown.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(thrown.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(thrown.fields()).containsExactly(expected);
	}

	private static void assertNotFound(BusinessException thrown) {
		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(thrown.status()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}

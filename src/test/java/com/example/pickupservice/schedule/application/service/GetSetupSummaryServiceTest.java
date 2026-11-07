package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.SetupChild;
import com.example.pickupservice.schedule.application.port.in.SetupSchedule;
import com.example.pickupservice.schedule.application.port.in.SetupSpot;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import com.example.pickupservice.schedule.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.LoadFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetSetupSummaryServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long OTHER_FAMILY_ID = 20L;
	private static final long CHILD_ID = 101L;
	private static final String CHILD_NAME = "민준";
	private static final long ACADEMY_ID = 301L;
	private static final long OTHER_ACADEMY_ID = 302L;
	private static final long SCHEDULE_ID = 501L;
	private static final long OTHER_SCHEDULE_ID = 502L;
	private static final long SPOT_ID = 701L;
	private static final long OTHER_SPOT_ID = 702L;
	private static final int SETTINGS_VERSION = 7;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);

	private static final ChildQuery QUERY = new ChildQuery(FAMILY_ID, CHILD_ID);

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadFamilyUseCase loadFamilyUseCase;

	@Mock
	private LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;

	@Mock
	private LoadSchedulePort loadSchedulePort;

	@Mock
	private LoadPickupSpotPort loadPickupSpotPort;

	private GetSetupSummaryService getSetupSummaryService;

	@BeforeEach
	void setUp() {
		getSetupSummaryService = new GetSetupSummaryService(loadMemberUseCase, loadFamilyUseCase,
				loadMemberDeviceStatusPort, loadSchedulePort, loadPickupSpotPort);
	}

	@Test
	@DisplayName("설정 요약은 기기 · READY 일정 · 기본 장소가 다 갖춰지면 missing이 비어 있고 자녀 · 일정 · 장소를 담는다")
	void getSetupSummaryReturnsEmptyMissingAndDetailsWhenEverythingIsSet() {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.AUTO, SETTINGS_VERSION);
		givenDeviceStatus(DeviceStatus.CONNECTED);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				schedule(SCHEDULE_ID, academy(ACADEMY_ID, "해솔영어학원"), ScheduleStatus.READY),
				schedule(OTHER_SCHEDULE_ID, academy(OTHER_ACADEMY_ID, "한빛수학학원"), ScheduleStatus.READY)));
		given(loadPickupSpotPort.loadSpots(eq(FAMILY_ID), anyList())).willReturn(List.of(
				new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true),
				new SpotSummary(OTHER_SPOT_ID, OTHER_ACADEMY_ID, "후문 앞", true)));

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.missing()).isEmpty();
		assertThat(summary.child()).isEqualTo(new SetupChild(CHILD_ID, CHILD_NAME, DeviceStatus.CONNECTED));
		assertThat(summary.schedules()).containsExactlyInAnyOrder(
				new SetupSchedule(SCHEDULE_ID, "해솔영어학원", ScheduleStatus.READY),
				new SetupSchedule(OTHER_SCHEDULE_ID, "한빛수학학원", ScheduleStatus.READY));
		assertThat(summary.pickupSpots()).containsExactlyInAnyOrder(
				new SetupSpot(SPOT_ID, "정문 앞", true),
				new SetupSpot(OTHER_SPOT_ID, "후문 앞", true));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Long>> academyIdsCaptor = ArgumentCaptor.forClass(List.class);
		verify(loadPickupSpotPort).loadSpots(eq(FAMILY_ID), academyIdsCaptor.capture());
		assertThat(academyIdsCaptor.getValue()).containsExactlyInAnyOrder(ACADEMY_ID, OTHER_ACADEMY_ID);
	}

	@ParameterizedTest
	@EnumSource(value = DeviceStatus.class, names = {"INVITE_PENDING", "INVITE_EXPIRED", "NONE"})
	@DisplayName("설정 요약은 자녀 기기가 CONNECTED가 아니면 missing에 CHILD_DEVICE를 넣는다")
	void getSetupSummaryAddsChildDeviceToMissingWhenDeviceIsNotConnected(DeviceStatus deviceStatus) {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.AUTO, SETTINGS_VERSION);
		givenDeviceStatus(deviceStatus);
		givenReadyScheduleWithDefaultSpot();

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.missing()).containsExactly(MissingSetupItem.CHILD_DEVICE);
		assertThat(summary.child()).isEqualTo(new SetupChild(CHILD_ID, CHILD_NAME, deviceStatus));
	}

	@Test
	@DisplayName("설정 요약은 일정이 DRAFT뿐이면 missing에 SCHEDULE을 넣는다")
	void getSetupSummaryAddsScheduleToMissingWhenOnlyDraftSchedulesExist() {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.AUTO, SETTINGS_VERSION);
		givenDeviceStatus(DeviceStatus.CONNECTED);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				schedule(SCHEDULE_ID, academy(ACADEMY_ID, "해솔영어학원"), ScheduleStatus.DRAFT)));
		given(loadPickupSpotPort.loadSpots(eq(FAMILY_ID), anyList())).willReturn(List.of(
				new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true)));

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.missing()).containsExactly(MissingSetupItem.SCHEDULE);
		assertThat(summary.schedules()).containsExactly(
				new SetupSchedule(SCHEDULE_ID, "해솔영어학원", ScheduleStatus.DRAFT));
	}

	@Test
	@DisplayName("설정 요약은 일정이 하나도 없으면 missing에 SCHEDULE만 넣고 PICKUP_SPOT은 넣지 않는다")
	void getSetupSummaryAddsOnlyScheduleToMissingWhenNoScheduleExists() {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.AUTO, SETTINGS_VERSION);
		givenDeviceStatus(DeviceStatus.CONNECTED);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of());

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.missing()).containsExactly(MissingSetupItem.SCHEDULE);
		assertThat(summary.schedules()).isEmpty();
		assertThat(summary.pickupSpots()).isEmpty();
	}

	@Test
	@DisplayName("설정 요약은 일정 학원 중 기본 장소가 없는 학원이 있으면 missing에 PICKUP_SPOT을 넣는다")
	void getSetupSummaryAddsPickupSpotToMissingWhenAnAcademyHasNoDefaultSpot() {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.AUTO, SETTINGS_VERSION);
		givenDeviceStatus(DeviceStatus.CONNECTED);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				schedule(SCHEDULE_ID, academy(ACADEMY_ID, "해솔영어학원"), ScheduleStatus.READY),
				schedule(OTHER_SCHEDULE_ID, academy(OTHER_ACADEMY_ID, "한빛수학학원"), ScheduleStatus.READY)));
		given(loadPickupSpotPort.loadSpots(eq(FAMILY_ID), anyList())).willReturn(List.of(
				new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true),
				new SpotSummary(OTHER_SPOT_ID, OTHER_ACADEMY_ID, "후문 앞", false)));

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.missing()).containsExactly(MissingSetupItem.PICKUP_SPOT);
		assertThat(summary.pickupSpots()).containsExactlyInAnyOrder(
				new SetupSpot(SPOT_ID, "정문 앞", true),
				new SetupSpot(OTHER_SPOT_ID, "후문 앞", false));
	}

	@Test
	@DisplayName("설정 요약은 일정 학원에 장소가 하나도 없으면 missing에 PICKUP_SPOT을 넣는다")
	void getSetupSummaryAddsPickupSpotToMissingWhenAcademyHasNoSpotAtAll() {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.AUTO, SETTINGS_VERSION);
		givenDeviceStatus(DeviceStatus.CONNECTED);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				schedule(SCHEDULE_ID, academy(ACADEMY_ID, "해솔영어학원"), ScheduleStatus.READY)));
		given(loadPickupSpotPort.loadSpots(eq(FAMILY_ID), anyList())).willReturn(List.of());

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.missing()).containsExactly(MissingSetupItem.PICKUP_SPOT);
		assertThat(summary.pickupSpots()).isEmpty();
	}

	@Test
	@DisplayName("설정 요약은 기기 미연결 · 일정 없음이 함께면 missing에 CHILD_DEVICE와 SCHEDULE을 모두 넣는다")
	void getSetupSummaryAddsAllMissingItemsWhenSeveralAreMissing() {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.AUTO, SETTINGS_VERSION);
		givenDeviceStatus(DeviceStatus.NONE);
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of());

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.missing())
				.containsExactlyInAnyOrder(MissingSetupItem.CHILD_DEVICE, MissingSetupItem.SCHEDULE);
	}

	@Test
	@DisplayName("설정 요약은 가족의 joinCallMode와 settingsVersion을 담는다")
	void getSetupSummaryContainsJoinCallModeAndFamilySettingsVersion() {
		givenChild(FAMILY_ID, Role.CHILD);
		givenFamily(JoinCallMode.GUARDIAN_CONFIRM, 12);
		givenDeviceStatus(DeviceStatus.CONNECTED);
		givenReadyScheduleWithDefaultSpot();

		SetupSummary summary = getSetupSummaryService.getSetupSummary(QUERY);

		assertThat(summary.joinCallMode()).isEqualTo(JoinCallMode.GUARDIAN_CONFIRM);
		assertThat(summary.settingsVersion()).isEqualTo(12);
	}

	@Test
	@DisplayName("설정 요약은 다른 가족의 자녀면 NOT_FOUND로 거절한다")
	void getSetupSummaryRejectsWithNotFoundWhenChildBelongsToOtherFamily() {
		givenChild(OTHER_FAMILY_ID, Role.CHILD);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> getSetupSummaryService.getSetupSummary(QUERY));

		assertNotFound(thrown);
	}

	@Test
	@DisplayName("설정 요약은 childId가 CHILD가 아니면 NOT_FOUND로 거절한다")
	void getSetupSummaryRejectsWithNotFoundWhenMemberIsNotChild() {
		givenChild(FAMILY_ID, Role.GUARDIAN);

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> getSetupSummaryService.getSetupSummary(QUERY));

		assertNotFound(thrown);
	}

	@Test
	@DisplayName("설정 요약은 없는 childId면 NOT_FOUND로 거절한다")
	void getSetupSummaryRejectsWithNotFoundWhenMemberDoesNotExist() {
		given(loadMemberUseCase.findMember(CHILD_ID)).willReturn(Optional.empty());

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> getSetupSummaryService.getSetupSummary(QUERY));

		assertNotFound(thrown);
	}

	private void givenChild(long familyId, Role role) {
		given(loadMemberUseCase.findMember(CHILD_ID))
				.willReturn(Optional.of(new MemberInfo(CHILD_ID, familyId, role, CHILD_NAME, "010-0000-0000")));
	}

	private void givenFamily(JoinCallMode joinCallMode, int settingsVersion) {
		given(loadFamilyUseCase.findFamily(FAMILY_ID))
				.willReturn(new FamilyInfo(FAMILY_ID, joinCallMode, settingsVersion));
	}

	private void givenDeviceStatus(DeviceStatus deviceStatus) {
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList()))
				.willReturn(Map.of(CHILD_ID, deviceStatus));
	}

	private void givenReadyScheduleWithDefaultSpot() {
		given(loadSchedulePort.loadSchedulesOfChild(CHILD_ID)).willReturn(List.of(
				schedule(SCHEDULE_ID, academy(ACADEMY_ID, "해솔영어학원"), ScheduleStatus.READY)));
		given(loadPickupSpotPort.loadSpots(eq(FAMILY_ID), anyList())).willReturn(List.of(
				new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", true)));
	}

	private static Academy academy(long academyId, String name) {
		return Academy.restore(academyId, "kakao:" + academyId, name, "서울 강남구 테헤란로 152", LOCATION, ENTRANCE, "02-555-1234");
	}

	private static PickupSchedule schedule(long scheduleId, Academy academy, ScheduleStatus status) {
		return PickupSchedule.restore(scheduleId, CHILD_ID, academy, Set.of(DayOfWeek.MONDAY),
				LocalTime.of(18, 0), 5, status);
	}

	private static void assertNotFound(BusinessException thrown) {
		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(thrown.status()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}

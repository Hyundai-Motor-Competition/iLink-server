package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.DailyPickups;
import com.example.pickupservice.schedule.application.port.in.PickupReadiness;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickup;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickups;
import com.example.pickupservice.schedule.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetWeeklyPickupsServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long GUARDIAN_ID = 100L;
	private static final long MINJUN_ID = 101L;
	private static final long SEOYEON_ID = 102L;
	private static final long ACADEMY_ID = 301L;
	private static final long OTHER_ACADEMY_ID = 302L;
	private static final long SCHEDULE_ID = 501L;
	private static final long OTHER_SCHEDULE_ID = 502L;
	private static final long SPOT_ID = 701L;
	private static final long OTHER_SPOT_ID = 702L;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);

	// UTC 2026-11-08(일) 16:00 = 한국 2026-11-09(월) 01:00
	private static final Instant KST_MONDAY_UTC_SUNDAY = Instant.parse("2026-11-08T16:00:00Z");
	private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadSchedulePort loadSchedulePort;

	@Mock
	private LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;

	@Mock
	private LoadPickupSpotPort loadPickupSpotPort;

	// #33

	@ParameterizedTest
	@CsvSource({
			"2026-11-08T16:00:00Z, 2026-11-09",
			"2026-11-11T03:00:00Z, 2026-11-09",
			"2026-11-15T14:59:59Z, 2026-11-09",
			"2026-11-08T14:00:00Z, 2026-11-02"
	})
	@DisplayName("주간 일정은 weekStart가 없으면 한국 날짜 기준 이번 주 월요일부터 7일을 돌려준다")
	void getWeeklyPickupsStartsFromKstMondayOfThisWeekWhenWeekStartIsMissing(String now, LocalDate expectedMonday) {
		givenFamilyMembers();
		givenSchedules();
		givenDeviceStatuses(Map.of());
		givenSpots();

		WeeklyPickups weekly = service(Instant.parse(now)).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.weekStart()).isEqualTo(expectedMonday);
		assertThat(weekly.days()).extracting(DailyPickups::date).containsExactly(
				expectedMonday,
				expectedMonday.plusDays(1),
				expectedMonday.plusDays(2),
				expectedMonday.plusDays(3),
				expectedMonday.plusDays(4),
				expectedMonday.plusDays(5),
				expectedMonday.plusDays(6));
	}

	@Test
	@DisplayName("주간 일정은 weekStart가 있으면 그 날짜부터 7일을 돌려준다")
	void getWeeklyPickupsStartsFromGivenWeekStart() {
		givenFamilyMembers();
		givenSchedules();
		givenDeviceStatuses(Map.of());
		givenSpots();
		LocalDate givenWeekStart = LocalDate.of(2026, 11, 2);

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, givenWeekStart);

		assertThat(weekly.weekStart()).isEqualTo(givenWeekStart);
		assertThat(weekly.days()).hasSize(7);
		assertThat(weekly.days().get(0).date()).isEqualTo(givenWeekStart);
		assertThat(weekly.days().get(6).date()).isEqualTo(LocalDate.of(2026, 11, 8));
	}

	// #34

	@Test
	@DisplayName("주간 일정은 요일이 맞는 날짜에만 그 일정을 넣고 하루 안에서는 classEndTime 오름차순이다")
	void getWeeklyPickupsPutsScheduleOnlyOnMatchingWeekdays() {
		givenFamilyMembers();
		givenSchedules(
				schedule(SCHEDULE_ID, MINJUN_ID, academy(ACADEMY_ID, "해솔영어학원"),
						Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY),
				schedule(OTHER_SCHEDULE_ID, SEOYEON_ID, academy(OTHER_ACADEMY_ID, "한빛수학학원"),
						Set.of(DayOfWeek.WEDNESDAY), LocalTime.of(17, 0), 10, ScheduleStatus.READY));
		givenDeviceStatuses(Map.of(MINJUN_ID, DeviceStatus.CONNECTED, SEOYEON_ID, DeviceStatus.CONNECTED));
		givenSpots(defaultSpot(SPOT_ID, ACADEMY_ID), defaultSpot(OTHER_SPOT_ID, OTHER_ACADEMY_ID));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, MONDAY);

		WeeklyPickup minjun = new WeeklyPickup(SCHEDULE_ID, "민준", "해솔영어학원", LocalTime.of(18, 0),
				LocalTime.of(18, 5));
		WeeklyPickup seoyeon = new WeeklyPickup(OTHER_SCHEDULE_ID, "서연", "한빛수학학원", LocalTime.of(17, 0),
				LocalTime.of(17, 10));
		assertThat(weekly.days()).containsExactly(
				new DailyPickups(MONDAY, List.of(minjun)),
				new DailyPickups(MONDAY.plusDays(1), List.of()),
				new DailyPickups(MONDAY.plusDays(2), List.of(seoyeon, minjun)),
				new DailyPickups(MONDAY.plusDays(3), List.of()),
				new DailyPickups(MONDAY.plusDays(4), List.of()),
				new DailyPickups(MONDAY.plusDays(5), List.of()),
				new DailyPickups(MONDAY.plusDays(6), List.of()));
	}

	@Test
	@DisplayName("주간 일정은 가족의 CHILD 회원들의 일정만 한 번에 읽는다")
	void getWeeklyPickupsLoadsSchedulesOfChildMembersOnly() {
		givenFamilyMembers();
		givenSchedules();
		givenDeviceStatuses(Map.of());
		givenSpots();

		service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, MONDAY);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Long>> childIdsCaptor = ArgumentCaptor.forClass(List.class);
		verify(loadSchedulePort).loadSchedulesOfChildren(childIdsCaptor.capture());
		assertThat(childIdsCaptor.getValue()).containsExactlyInAnyOrder(MINJUN_ID, SEOYEON_ID);
	}

	@Test
	@DisplayName("주간 일정은 DRAFT 일정도 요일이 맞는 날짜에 넣는다")
	void getWeeklyPickupsIncludesDraftSchedules() {
		givenFamilyMembers();
		givenSchedules(schedule(SCHEDULE_ID, MINJUN_ID, academyWithoutEntrance(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.FRIDAY), LocalTime.of(18, 0), 5, ScheduleStatus.DRAFT));
		givenDeviceStatuses(Map.of(MINJUN_ID, DeviceStatus.CONNECTED));
		givenSpots(defaultSpot(SPOT_ID, ACADEMY_ID));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, MONDAY);

		assertThat(weekly.days().get(4)).isEqualTo(new DailyPickups(MONDAY.plusDays(4), List.of(
				new WeeklyPickup(SCHEDULE_ID, "민준", "해솔영어학원", LocalTime.of(18, 0), LocalTime.of(18, 5)))));
	}

	// #35

	@Test
	@DisplayName("주간 일정 readiness는 오늘 일정 자녀의 기기가 연결되고 학원 기본 장소가 있으면 startable true와 빈 missing을 돌려준다")
	void readinessIsStartableWhenDeviceAndDefaultSpotAreSet() {
		givenFamilyMembers();
		givenSchedules(mondayScheduleOfMinjun());
		givenDeviceStatuses(Map.of(MINJUN_ID, DeviceStatus.CONNECTED));
		givenSpots(defaultSpot(SPOT_ID, ACADEMY_ID));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.readiness()).isEqualTo(new PickupReadiness(true, List.of()));
	}

	@ParameterizedTest
	@EnumSource(value = DeviceStatus.class, names = {"INVITE_PENDING", "INVITE_EXPIRED", "NONE"})
	@DisplayName("주간 일정 readiness는 오늘 일정 자녀의 기기가 CONNECTED가 아니면 startable false와 CHILD_DEVICE를 돌려준다")
	void readinessIsNotStartableWhenChildDeviceIsNotConnected(DeviceStatus deviceStatus) {
		givenFamilyMembers();
		givenSchedules(mondayScheduleOfMinjun());
		givenDeviceStatuses(Map.of(MINJUN_ID, deviceStatus));
		givenSpots(defaultSpot(SPOT_ID, ACADEMY_ID));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.readiness()).isEqualTo(new PickupReadiness(false, List.of(MissingSetupItem.CHILD_DEVICE)));
	}

	@Test
	@DisplayName("주간 일정 readiness는 자녀 기기 상태가 없으면 CHILD_DEVICE를 돌려준다")
	void readinessTreatsMissingDeviceStatusAsNotConnected() {
		givenFamilyMembers();
		givenSchedules(mondayScheduleOfMinjun());
		givenDeviceStatuses(Map.of());
		givenSpots(defaultSpot(SPOT_ID, ACADEMY_ID));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.readiness()).isEqualTo(new PickupReadiness(false, List.of(MissingSetupItem.CHILD_DEVICE)));
	}

	@Test
	@DisplayName("주간 일정 readiness는 오늘 일정 학원에 기본 장소가 없으면 startable false와 PICKUP_SPOT을 돌려준다")
	void readinessIsNotStartableWhenTodayAcademyHasNoDefaultSpot() {
		givenFamilyMembers();
		givenSchedules(mondayScheduleOfMinjun());
		givenDeviceStatuses(Map.of(MINJUN_ID, DeviceStatus.CONNECTED));
		givenSpots(new SpotSummary(SPOT_ID, ACADEMY_ID, "정문 앞", false));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.readiness()).isEqualTo(new PickupReadiness(false, List.of(MissingSetupItem.PICKUP_SPOT)));
	}

	@Test
	@DisplayName("주간 일정 readiness는 기기와 기본 장소가 둘 다 빠지면 missing에 둘 다 넣는다")
	void readinessListsBothMissingItems() {
		givenFamilyMembers();
		givenSchedules(mondayScheduleOfMinjun());
		givenDeviceStatuses(Map.of(MINJUN_ID, DeviceStatus.NONE));
		givenSpots();

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.readiness().startable()).isFalse();
		assertThat(weekly.readiness().missing())
				.containsExactlyInAnyOrder(MissingSetupItem.CHILD_DEVICE, MissingSetupItem.PICKUP_SPOT);
	}

	@Test
	@DisplayName("주간 일정 readiness는 한국 날짜 기준 오늘 일정이 있는 자녀와 학원만 본다")
	void readinessChecksOnlyChildrenAndAcademiesWithScheduleToday() {
		givenFamilyMembers();
		givenSchedules(
				mondayScheduleOfMinjun(),
				schedule(OTHER_SCHEDULE_ID, SEOYEON_ID, academy(OTHER_ACADEMY_ID, "한빛수학학원"),
						Set.of(DayOfWeek.SUNDAY), LocalTime.of(17, 0), 10, ScheduleStatus.READY));
		givenDeviceStatuses(Map.of(MINJUN_ID, DeviceStatus.CONNECTED, SEOYEON_ID, DeviceStatus.NONE));
		givenSpots(defaultSpot(SPOT_ID, ACADEMY_ID));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.readiness()).isEqualTo(new PickupReadiness(true, List.of()));
	}

	@Test
	@DisplayName("주간 일정 readiness는 오늘 일정이 없으면 startable false와 빈 missing을 돌려준다")
	void readinessIsNotStartableWithEmptyMissingWhenNoScheduleToday() {
		givenFamilyMembers();
		givenSchedules(schedule(SCHEDULE_ID, MINJUN_ID, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.TUESDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY));
		givenDeviceStatuses(Map.of(MINJUN_ID, DeviceStatus.CONNECTED));
		givenSpots(defaultSpot(SPOT_ID, ACADEMY_ID));

		WeeklyPickups weekly = service(KST_MONDAY_UTC_SUNDAY).getWeeklyPickups(FAMILY_ID, null);

		assertThat(weekly.readiness()).isEqualTo(new PickupReadiness(false, List.of()));
	}

	private GetWeeklyPickupsService service(Instant now) {
		return new GetWeeklyPickupsService(loadMemberUseCase, loadSchedulePort, loadMemberDeviceStatusPort,
				loadPickupSpotPort, Clock.fixed(now, ZoneOffset.UTC));
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

	private void givenDeviceStatuses(Map<Long, DeviceStatus> statuses) {
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).willReturn(new HashMap<>(statuses));
	}

	private void givenSpots(SpotSummary... spots) {
		given(loadPickupSpotPort.loadSpots(eq(FAMILY_ID), anyList())).willReturn(List.of(spots));
	}

	private static PickupSchedule mondayScheduleOfMinjun() {
		return schedule(SCHEDULE_ID, MINJUN_ID, academy(ACADEMY_ID, "해솔영어학원"),
				Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), 5, ScheduleStatus.READY);
	}

	private static SpotSummary defaultSpot(long spotId, long academyId) {
		return new SpotSummary(spotId, academyId, "정문 앞", true);
	}

	private static Academy academy(long academyId, String name) {
		return Academy.restore(academyId, name, "서울 강남구 테헤란로 152", LOCATION, ENTRANCE, "02-555-1234");
	}

	private static Academy academyWithoutEntrance(long academyId, String name) {
		return Academy.restore(academyId, name, "서울 강남구 테헤란로 152", LOCATION, null, "02-555-1234");
	}

	private static PickupSchedule schedule(long scheduleId, long childId, Academy academy, Set<DayOfWeek> weekdays,
			LocalTime classEndTime, int targetPickup, ScheduleStatus status) {
		return PickupSchedule.restore(scheduleId, childId, academy, weekdays, classEndTime, targetPickup, status);
	}
}

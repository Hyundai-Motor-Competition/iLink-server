package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ContactResult;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSchedulePort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.schedule.application.port.in.AcademyInfo;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GetContactsServiceTest {

	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long DELEGATE_ID = 35L;
	private static final long TEACHER_ID = 36L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long ACADEMY_ID = 901L;
	private static final long FRONT_GATE_SPOT_ID = 21L;

	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_VEHICLE_ID = 78L;
	private static final long OTHER_FAMILY_GUARDIAN_ID = 41L;

	private static final String GUARDIAN_PHONE = "010-1234-5678";
	private static final String CO_GUARDIAN_PHONE = "010-2222-3333";
	private static final String TEACHER_PHONE = "010-4444-5555";
	private static final String MINJUN_PHONE = "010-7777-8888";
	private static final String ACADEMY_NAME = "해법수학 역삼점";
	private static final String ACADEMY_PHONE = "02-555-1234";

	private static final LatLng FRONT_STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng FRONT_MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final LatLng ACADEMY_LOCATION = new LatLng(37.501200, 127.039800);
	private static final LatLng ACADEMY_ENTRANCE = new LatLng(37.501250, 127.039850);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadSchedulePort loadSchedulePort;

	private GetContactsService getContactsService;

	@BeforeEach
	void setUp() {
		getContactsService = new GetContactsService(loadSessionPort, loadFamilyPort, loadSchedulePort);
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session()));
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 0));
		// 대리인은 전화번호가 없고, 민준은 자녀지만 전화번호가 있다
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", GUARDIAN_PHONE),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이보호", CO_GUARDIAN_PHONE),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", null),
				new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "최선생", TEACHER_PHONE),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", MINJUN_PHONE),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		// 형제의 일정 두 개가 같은 학원(한 세션은 같은 학원 일정만 묶는다)
		lenient().when(loadSchedulePort.loadSchedules(anyList())).thenReturn(List.of(
				schedule(MINJUN_SCHEDULE_ID, MINJUN_ID),
				schedule(SEOYUN_SCHEDULE_ID, SEOYUN_ID)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("principalsWhoCanSeeSession")
	@DisplayName("연락처는 전화번호가 있는 가족 멤버(CHILD 제외)와 role ACADEMY인 학원 대표번호를 돌려준다")
	void getContactsReturnsMembersWithPhoneExceptChildrenAndAcademyPhone(AuthPrincipal principal) {
		List<ContactResult> contacts = getContactsService.getContacts(new SessionQuery(principal, SESSION_ID));

		assertThat(contacts).containsExactlyInAnyOrder(
				new ContactResult("김보호", "GUARDIAN", GUARDIAN_PHONE),
				new ContactResult("이보호", "CO_GUARDIAN", CO_GUARDIAN_PHONE),
				new ContactResult("최선생", "TEACHER", TEACHER_PHONE),
				new ContactResult(ACADEMY_NAME, "ACADEMY", ACADEMY_PHONE));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("principalsOutsideSession")
	@DisplayName("연락처는 세션을 볼 수 없는 주체(다른 가족의 차량 · 사용자)면 NOT_FOUND로 거절한다")
	void getContactsRejectsPrincipalOutsideSessionWithNotFound(AuthPrincipal principal) {
		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> getContactsService.getContacts(new SessionQuery(principal, SESSION_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
	}

	// SES-10 호출 주체: 그 세션의 차량, 가족 사용자
	static Stream<Named<AuthPrincipal>> principalsWhoCanSeeSession() {
		return Stream.of(
				Named.of("그 세션의 차량", new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null)),
				Named.of("가족 보호자", new AuthPrincipal(PrincipalType.USER, GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN)),
				Named.of("세션 자녀", new AuthPrincipal(PrincipalType.USER, MINJUN_ID, FAMILY_ID, Role.CHILD)));
	}

	static Stream<Named<AuthPrincipal>> principalsOutsideSession() {
		return Stream.of(
				Named.of("다른 가족의 차량",
						new AuthPrincipal(PrincipalType.VEHICLE, OTHER_FAMILY_VEHICLE_ID, OTHER_FAMILY_ID, null)),
				Named.of("다른 가족의 보호자",
						new AuthPrincipal(PrincipalType.USER, OTHER_FAMILY_GUARDIAN_ID, OTHER_FAMILY_ID,
								Role.GUARDIAN)));
	}

	private static ScheduleInfo schedule(long scheduleId, long childUserId) {
		AcademyInfo academy = new AcademyInfo(ACADEMY_ID, ACADEMY_NAME, "서울 강남구 역삼로 123", ACADEMY_LOCATION,
				ACADEMY_ENTRANCE, ACADEMY_PHONE);
		return new ScheduleInfo(scheduleId, childUserId, academy, Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
				LocalTime.of(18, 0), 10, ScheduleStatus.READY);
	}

	private static PickupSession session() {
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(7002L, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, FRONT_GATE_SPOT_ID, "정문 앞",
				FRONT_STOP_POINT, FRONT_MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.ROUTING,
				SpotStatus.SELECTED, 0, 0, 0, null, false, null, null, null, null, null, null, null, null, null,
				null, null, children);
	}

}

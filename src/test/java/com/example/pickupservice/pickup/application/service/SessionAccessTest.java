package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SessionAccessTest {

	// 한국 시간 2026-11-09(월) 17:00
	private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 9);

	private static final long SESSION_ID = 501L;
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long DELEGATE_ID = 35L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SEOYUN_SCHEDULE_ID = 802L;
	private static final long SPOT_ID = 21L;
	private static final String SPOT_NAME = "해솔영어학원 정문 앞";
	private static final int SPOT_VERSION = 0;
	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);
	private static final Instant ETA_AT = NOW.plusSeconds(170);
	private static final Instant ETA_UPDATED_AT = NOW.minusSeconds(5);
	private static final int REMAINING_METERS = 650;
	private static final LatLng MINJUN_LOCATION = new LatLng(37.501100, 127.039500);
	private static final Instant MINJUN_LOCATED_AT = NOW.minusSeconds(10);

	// 서버의 지금 stateVersion
	private static final int SERVER_STATE_VERSION = 6;

	private static final long CALL_ID = 3301L;
	private static final Instant CALL_ACCEPTED_AT = NOW.minusSeconds(40);
	private static final Instant CALL_SENT_AT = NOW.minusSeconds(38);

	// 이 세션을 볼 수 없는 주체용
	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_VEHICLE_ID = 78L;
	private static final long SAME_FAMILY_OTHER_VEHICLE_ID = 79L;
	private static final long OTHER_FAMILY_GUARDIAN_ID = 91L;
	private static final long OTHER_FAMILY_CHILD_ID = 92L;
	private static final long SIBLING_NOT_IN_SESSION_ID = 36L;

	private static final FamilyInfo FAMILY = new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 1);
	private static final List<MemberInfo> MEMBERS = List.of(
			new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1111-2222"),
			new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444"),
			new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666"),
			new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", "010-7777-8888"),
			new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", "010-9999-0000"));
	private static final Call SENT_CALL = Call.restore(CALL_ID, SESSION_ID, null, CallTriggerType.ETA_3MIN,
			CallStatus.SENT, SPOT_VERSION, CALL_ACCEPTED_AT, CALL_SENT_AT, null, null, null, null);

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadCallPort loadCallPort;

	private SessionAccess sessionAccess;

	@BeforeEach
	void setUp() {
		sessionAccess = new SessionAccess(loadFamilyPort, loadCallPort, Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID)).thenReturn(FAMILY);
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(MEMBERS);
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.of(SENT_CALL));
	}

	@ParameterizedTest(name = "요청 {0} · 서버 6")
	@ValueSource(ints = {5, 7})
	@DisplayName("세션을 바꾸는 요청의 stateVersion이 서버와 다르면 STATE_VERSION_CONFLICT와 최신 세션 뷰로 거절한다")
	void requireStateVersionRejectsDifferentVersionWithConflictAndLatestView(int requestedStateVersion) {
		PickupSession session = sessionWithCalledChildren();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> sessionAccess.requireStateVersion(session, requestedStateVersion, vehicle()));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("STATE_VERSION_CONFLICT");
		// 최신 세션 뷰: 요청한 주체 기준으로, 가장 최근 호출까지 반영한 지금 서버 상태
		assertThat(exception.session()).isInstanceOfSatisfying(SessionView.class, latest -> {
			assertThat(latest.stateVersion()).isEqualTo(SERVER_STATE_VERSION);
			assertThat(latest.status().call()).isEqualTo("SENT");
			assertThat(latest).isEqualTo(SessionViewAssembler.assemble(session, vehicle(), FAMILY, MEMBERS,
					Optional.of(SENT_CALL), NOW));
		});
	}

	@Test
	@DisplayName("세션을 바꾸는 요청의 stateVersion이 서버와 같으면 통과시킨다")
	void requireStateVersionPassesSameVersion() {
		PickupSession session = sessionWithCalledChildren();

		assertThatCode(() -> sessionAccess.requireStateVersion(session, SERVER_STATE_VERSION, vehicle()))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("principalsOutsideSession")
	@DisplayName("세션 접근 확인은 이 세션을 볼 수 없는 주체를 NOT_FOUND로 거절한다")
	void requireVisibleRejectsPrincipalOutsideSessionWithNotFound(AuthPrincipal principal) {
		PickupSession session = sessionWithCalledChildren();

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> SessionAccess.requireVisible(session, principal));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("principalsInSession")
	@DisplayName("세션 접근 확인은 그 세션의 차량 · 가족 사용자 · 세션에 있는 자녀를 통과시킨다")
	void requireVisiblePassesSessionVehicleFamilyUsersAndIncludedChild(AuthPrincipal principal) {
		PickupSession session = sessionWithCalledChildren();

		assertThatCode(() -> SessionAccess.requireVisible(session, principal)).doesNotThrowAnyException();
	}

	// 차량은 vehicleId, 사용자는 familyId, 자녀는 세션에 자기가 있는지로 본다
	static Stream<Named<AuthPrincipal>> principalsOutsideSession() {
		return Stream.of(
				Named.of("다른 가족의 차량",
						new AuthPrincipal(PrincipalType.VEHICLE, OTHER_FAMILY_VEHICLE_ID, OTHER_FAMILY_ID, null)),
				Named.of("같은 가족의 다른 차량",
						new AuthPrincipal(PrincipalType.VEHICLE, SAME_FAMILY_OTHER_VEHICLE_ID, FAMILY_ID, null)),
				Named.of("다른 가족의 GUARDIAN",
						new AuthPrincipal(PrincipalType.USER, OTHER_FAMILY_GUARDIAN_ID, OTHER_FAMILY_ID, Role.GUARDIAN)),
				Named.of("다른 가족의 자녀",
						new AuthPrincipal(PrincipalType.USER, OTHER_FAMILY_CHILD_ID, OTHER_FAMILY_ID, Role.CHILD)),
				Named.of("세션에 없는 같은 가족 자녀", user(SIBLING_NOT_IN_SESSION_ID, Role.CHILD)));
	}

	static Stream<Named<AuthPrincipal>> principalsInSession() {
		return Stream.of(
				Named.of("그 세션의 차량", vehicle()),
				Named.of("GUARDIAN", user(GUARDIAN_ID, Role.GUARDIAN)),
				Named.of("CO_GUARDIAN", user(CO_GUARDIAN_ID, Role.CO_GUARDIAN)),
				Named.of("DELEGATE", user(DELEGATE_ID, Role.DELEGATE)),
				Named.of("세션에 있는 자녀", user(MINJUN_ID, Role.CHILD)));
	}

	private static AuthPrincipal vehicle() {
		return new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	}

	private static AuthPrincipal user(long userId, Role role) {
		return new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
	}

	// SES-04 뒤 합류 호출이 발송된 세션: stateVersion 6, 민준 · 서윤 모두 CALLED
	private static PickupSession sessionWithCalledChildren() {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, VehiclePhase.APPROACHING, SpotStatus.CONFIRMED,
				SERVER_STATE_VERSION, SPOT_VERSION, 1, SPOT_VERSION, false, ETA_AT, REMAINING_METERS,
				ETA_UPDATED_AT, null, null, null, null, null, null, null, List.of(
						SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
								MINJUN_LOCATION, MINJUN_LOCATED_AT, null, null),
						SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
								null, null, null, null)));
	}

}

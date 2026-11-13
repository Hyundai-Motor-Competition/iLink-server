package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CallGuardView;
import com.example.pickupservice.pickup.application.port.in.ChildLocationView;
import com.example.pickupservice.pickup.application.port.in.SessionChildView;
import com.example.pickupservice.pickup.application.port.in.SessionDriverView;
import com.example.pickupservice.pickup.application.port.in.SessionIncidentView;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.in.SessionSpotView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.in.VehicleLocationView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.IncidentType;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSessionServiceTest {

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

	private static final Instant ETA_AT = NOW.plusSeconds(720);
	private static final Instant ETA_UPDATED_AT = NOW.minusSeconds(5);
	private static final int REMAINING_METERS = 4200;
	private static final Instant INCIDENT_OPENED_AT = NOW.minusSeconds(40);

	private static final LatLng MINJUN_LOCATION = new LatLng(37.501100, 127.039500);
	private static final Instant MINJUN_LOCATED_AT = NOW.minusSeconds(10);
	private static final LatLng SEOYUN_LOCATION = new LatLng(37.501050, 127.039450);
	private static final Instant SEOYUN_LOCATED_AT = NOW.minusSeconds(12);
	private static final Instant MINJUN_ACKNOWLEDGED_AT = NOW.minusSeconds(30);
	private static final Instant MINJUN_READY_AT = NOW.minusSeconds(45);

	// 마지막 차량 위치(SIG-01 location)와 그 시각
	private static final LatLng VEHICLE_LOCATION = new LatLng(37.498100, 127.027600);
	private static final Instant VEHICLE_LOCATED_AT = NOW.minusSeconds(3);
	private static final VehicleLocationView VEHICLE_LOCATION_VIEW = new VehicleLocationView(
			VEHICLE_LOCATION.lat(), VEHICLE_LOCATION.lng(), VEHICLE_LOCATED_AT);

	// 멤버 프로필 사진 서명 URL(V03 · C03 아바타, C02 driver)
	private static final String GUARDIAN_PROFILE_IMAGE_URL = "https://storage.example.com/profiles/31.jpg?sig=a";
	private static final String CO_GUARDIAN_PROFILE_IMAGE_URL = "https://storage.example.com/profiles/32.jpg?sig=b";
	private static final String MINJUN_PROFILE_IMAGE_URL = "https://storage.example.com/profiles/33.jpg?sig=c";

	// 이 세션을 볼 수 없는 주체용
	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_VEHICLE_ID = 78L;
	private static final long SAME_FAMILY_OTHER_VEHICLE_ID = 79L;
	private static final long OTHER_FAMILY_GUARDIAN_ID = 91L;
	private static final long OTHER_FAMILY_CHILD_ID = 92L;
	private static final long SIBLING_NOT_IN_SESSION_ID = 36L;

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadCallPort loadCallPort;

	private GetSessionService getSessionService;

	@BeforeEach
	void setUp() {
		getSessionService = new GetSessionService(loadSessionPort, loadFamilyPort, loadCallPort,
				new SessionViewAssembler(PickupProperties.defaults()), Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(loadFamilyPort.loadFamily(FAMILY_ID))
				.thenReturn(new FamilyInfo(FAMILY_ID, JoinCallMode.AUTO, 1));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1111-2222"),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444"),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", "010-7777-8888"),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", "010-9999-0000")));
		lenient().when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.empty());
	}

	@Test
	@DisplayName("세션 조회는 자녀 주체에게 자기 children 항목만 돌려준다")
	void getSessionReturnsOnlyOwnChildEntryForChildPrincipal() {
		givenSession(beforeRouteConfirmed());

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.children()).extracting(SessionChildView::childUserId).containsExactly(MINJUN_ID);
	}

	@Test
	@DisplayName("세션 조회는 SES-04 전이면 자녀 주체에게 spot을 null로 주고 eta.stopEtaAt은 준다")
	void getSessionGivesNullSpotButStopEtaAtToChildBeforeRouteConfirmed() {
		givenSession(beforeRouteConfirmed());

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.spot()).isNull();
		assertThat(view.eta().stopEtaAt()).isEqualTo(ETA_AT);
	}

	@Test
	@DisplayName("세션 조회는 SES-04 뒤면 자녀 주체에게도 spot을 준다")
	void getSessionGivesSpotToChildAfterRouteConfirmed() {
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.spot()).isEqualTo(new SessionSpotView(SPOT_ID, SPOT_NAME, SPOT_VERSION, false));
		assertThat(view.eta().stopEtaAt()).isEqualTo(ETA_AT);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vehicleAndGuardians")
	@DisplayName("세션 조회는 SES-04 전이어도 차량 · 보호자 주체에게는 spot을 준다")
	void getSessionGivesSpotToVehicleAndGuardiansBeforeRouteConfirmed(AuthPrincipal principal) {
		givenSession(beforeRouteConfirmed());

		SessionView view = getSessionService.getSession(new SessionQuery(principal, SESSION_ID));

		assertThat(view.spot()).isEqualTo(new SessionSpotView(SPOT_ID, SPOT_NAME, SPOT_VERSION, false));
	}

	@Test
	@DisplayName("세션 조회는 자녀 주체에게 children[].location을 주지 않는다")
	void getSessionHidesChildLocationFromChildPrincipal() {
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.children())
				.isNotEmpty()
				.allSatisfy(childView -> assertThat(childView.location()).isNull());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vehicleAndGuardians")
	@DisplayName("세션 조회는 차량 · 보호자 주체에게 children[].location을 준다")
	void getSessionGivesChildLocationToVehicleAndGuardians(AuthPrincipal principal) {
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(principal, SESSION_ID));

		assertThat(view.children())
				.extracting(SessionChildView::childUserId, SessionChildView::location)
				.containsExactlyInAnyOrder(
						tuple(MINJUN_ID, new ChildLocationView(MINJUN_LOCATION.lat(), MINJUN_LOCATION.lng(),
								MINJUN_LOCATED_AT)),
						tuple(SEOYUN_ID, new ChildLocationView(SEOYUN_LOCATION.lat(), SEOYUN_LOCATION.lng(),
								SEOYUN_LOCATED_AT)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vehicleAndGuardians")
	@DisplayName("세션 조회는 children[].readyAt에 자녀가 준비 완료를 누른 시각을 담고 누르지 않은 자녀는 null로 돌려준다")
	void getSessionGivesReadyAtInChildren(AuthPrincipal principal) {
		givenSession(afterRouteConfirmedWithReadyMinjun());

		SessionView view = getSessionService.getSession(new SessionQuery(principal, SESSION_ID));

		assertThat(view.children())
				.extracting(SessionChildView::childUserId, SessionChildView::readyAt)
				.containsExactlyInAnyOrder(tuple(MINJUN_ID, MINJUN_READY_AT), tuple(SEOYUN_ID, null));
	}

	@Test
	@DisplayName("세션 조회는 자녀 주체에게도 자기 children[].readyAt을 준다")
	void getSessionGivesOwnReadyAtToChildPrincipal() {
		givenSession(afterRouteConfirmedWithReadyMinjun());

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.children())
				.extracting(SessionChildView::childUserId, SessionChildView::readyAt)
				.containsExactly(tuple(MINJUN_ID, MINJUN_READY_AT));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vehicleAndGuardians")
	@DisplayName("세션 조회는 차량 · 보호자 주체에게 차량 위치가 있으면 남은 거리와 시간이 멀어도 vehicleLocation에 lat · lng · at을 늘 준다")
	void getSessionAlwaysGivesVehicleLocationToVehicleAndGuardians(AuthPrincipal principal) {
		// 남은 거리 4200m · 도착까지 720초: 자녀 기준(700m · 180초) 밖
		givenSession(sessionWithVehicleLocation(SessionStatus.ACTIVE, ETA_AT, REMAINING_METERS));

		SessionView view = getSessionService.getSession(new SessionQuery(principal, SESSION_ID));

		assertThat(view.vehicleLocation()).isEqualTo(VEHICLE_LOCATION_VIEW);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vehicleAndGuardians")
	@DisplayName("세션 조회는 저장된 차량 위치가 없으면 차량 · 보호자 주체에게도 vehicleLocation을 null로 준다")
	void getSessionGivesNullVehicleLocationWithoutStoredLocation(AuthPrincipal principal) {
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(principal, SESSION_ID));

		assertThat(view.vehicleLocation()).isNull();
	}

	@ParameterizedTest(name = "남은 거리 {0}m · 도착까지 {1}초 → 자녀에게 보임 {2}")
	@CsvSource({
			"700, 720, true",
			"701, 720, false",
			"4200, 180, true",
			"4200, 181, false",
			"701, 181, false",
			"650, 170, true"})
	@DisplayName("세션 조회는 자녀 주체에게 remainingMeters ≤ 700 또는 남은 시간 ≤ 180초일 때만 vehicleLocation을 주고 둘 다 넘으면 null로 준다")
	void getSessionGivesVehicleLocationToChildOnlyWithin700MetersOr180Seconds(int remainingMeters,
			int remainingSeconds, boolean shown) {
		givenSession(sessionWithVehicleLocation(SessionStatus.ACTIVE, NOW.plusSeconds(remainingSeconds),
				remainingMeters));

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.vehicleLocation()).isEqualTo(shown ? VEHICLE_LOCATION_VIEW : null);
	}

	@ParameterizedTest(name = "세션 {0}")
	@EnumSource(value = SessionStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("세션 조회는 끝난(COMPLETED · CANCELLED) 세션이면 차량 위치가 저장돼 있고 자녀 기준 안이어도 모든 주체에게 vehicleLocation을 null로 준다")
	void getSessionGivesNullVehicleLocationForFinishedSession(SessionStatus finished) {
		// 남은 거리 0m · 도착까지 0초: 자녀 기준 안
		givenSession(sessionWithVehicleLocation(finished, NOW, 0));

		for (AuthPrincipal principal : List.of(vehicle(), user(GUARDIAN_ID, Role.GUARDIAN), child(MINJUN_ID))) {
			SessionView view = getSessionService.getSession(new SessionQuery(principal, SESSION_ID));

			assertThat(view.vehicleLocation()).as("%s의 vehicleLocation", principal).isNull();
		}
	}

	@Test
	@DisplayName("세션 조회는 호출이 없으면 status.call NONE을 돌려준다")
	void getSessionReturnsCallStatusNoneWithoutCall() {
		givenSession(beforeRouteConfirmed());
		when(loadCallPort.loadLatestCall(SESSION_ID)).thenReturn(Optional.empty());

		SessionView view = getSessionService.getSession(new SessionQuery(vehicle(), SESSION_ID));

		assertThat(view.status().call()).isEqualTo("NONE");
	}

	@Test
	@DisplayName("세션 조회는 MEETING 자녀가 있으면 spot.locked true를 돌려준다")
	void getSessionReturnsLockedSpotWhenChildIsMeeting() {
		givenSession(afterRouteConfirmedWithMeetingChild());

		SessionView view = getSessionService.getSession(new SessionQuery(vehicle(), SESSION_ID));

		assertThat(view.spot()).isEqualTo(new SessionSpotView(SPOT_ID, SPOT_NAME, SPOT_VERSION, true));
	}

	@Test
	@DisplayName("세션 조회는 children[].profileImageUrl에 가족 멤버 정보의 profileImageUrl을 담고 없으면 null로 돌려준다")
	void getSessionGivesMemberProfileImageUrlInChildren() {
		givenMembers(membersWithProfileImages());
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(vehicle(), SESSION_ID));

		assertThat(view.children())
				.extracting(SessionChildView::childUserId, SessionChildView::profileImageUrl)
				.containsExactlyInAnyOrder(
						tuple(MINJUN_ID, MINJUN_PROFILE_IMAGE_URL),
						tuple(SEOYUN_ID, null));
	}

	@Test
	@DisplayName("세션 조회는 자녀 주체에게도 자기 children[].profileImageUrl을 준다")
	void getSessionGivesOwnProfileImageUrlToChildPrincipal() {
		givenMembers(membersWithProfileImages());
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.children())
				.extracting(SessionChildView::childUserId, SessionChildView::profileImageUrl)
				.containsExactly(tuple(MINJUN_ID, MINJUN_PROFILE_IMAGE_URL));
	}

	@Test
	@DisplayName("세션 조회는 driver에 세션 가족의 GUARDIAN 멤버 name · profileImageUrl을 담는다")
	void getSessionGivesGuardianAsDriverToVehicle() {
		givenMembers(membersWithProfileImages());
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(vehicle(), SESSION_ID));

		assertThat(view.driver()).isEqualTo(new SessionDriverView("김보호", GUARDIAN_PROFILE_IMAGE_URL));
	}

	@Test
	@DisplayName("세션 조회는 자녀 주체에게도 GUARDIAN 멤버를 driver로 준다")
	void getSessionGivesGuardianAsDriverToChildPrincipal() {
		givenMembers(membersWithProfileImages());
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(child(MINJUN_ID), SESSION_ID));

		assertThat(view.driver()).isEqualTo(new SessionDriverView("김보호", GUARDIAN_PROFILE_IMAGE_URL));
	}

	@Test
	@DisplayName("세션 조회는 가족에 GUARDIAN 멤버가 없으면 driver를 null로 돌려준다")
	void getSessionGivesNullDriverWithoutGuardian() {
		givenMembers(List.of(
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444",
						CO_GUARDIAN_PROFILE_IMAGE_URL, false),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666", null, false),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", "010-7777-8888", MINJUN_PROFILE_IMAGE_URL, false),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", "010-9999-0000", null, false)));
		givenSession(afterRouteConfirmedWithCalledChildren());

		SessionView view = getSessionService.getSession(new SessionQuery(vehicle(), SESSION_ID));

		assertThat(view.driver()).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("vehicleAndGuardians")
	@DisplayName("세션 조회는 열린 예외가 있으면 incident에 type · childUserId · openedAt을 채워 돌려준다")
	void getSessionFillsIncidentForOpenIncident(AuthPrincipal principal) {
		givenSession(sessionWithIncident(null));

		SessionView view = getSessionService.getSession(new SessionQuery(principal, SESSION_ID));

		assertThat(view.incident())
				.isEqualTo(new SessionIncidentView("CHILD_NOT_ON_BOARD", MINJUN_ID, INCIDENT_OPENED_AT));
	}

	@Test
	@DisplayName("세션 조회는 자녀 주체에게도 열린 예외의 incident를 채워 돌려준다")
	void getSessionFillsIncidentForChildPrincipal() {
		givenSession(sessionWithIncident(null));

		SessionView view = getSessionService.getSession(new SessionQuery(child(SEOYUN_ID), SESSION_ID));

		assertThat(view.incident())
				.isEqualTo(new SessionIncidentView("CHILD_NOT_ON_BOARD", MINJUN_ID, INCIDENT_OPENED_AT));
	}

	@Test
	@DisplayName("세션 조회는 예외가 없으면 incident를 null로 돌려준다")
	void getSessionReturnsNullIncidentWithoutIncident() {
		givenSession(beforeRouteConfirmed());

		SessionView view = getSessionService.getSession(new SessionQuery(vehicle(), SESSION_ID));

		assertThat(view.incident()).isNull();
	}

	@Test
	@DisplayName("세션 조회는 예외가 해소됐으면(incidentResolvedAt 있음) incident를 null로 돌려준다")
	void getSessionReturnsNullIncidentWhenIncidentResolved() {
		givenSession(sessionWithIncident(INCIDENT_OPENED_AT.plusSeconds(60)));

		SessionView view = getSessionService.getSession(new SessionQuery(vehicle(), SESSION_ID));

		assertThat(view.incident()).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("guardUnmetSessions")
	@DisplayName("세션 조회는 가드를 통과하지 못한 세션이면 call.guard에 met false와 통과하지 못한 가드 id를 unmet으로 돌려준다")
	void getSessionShowsUnmetGuardsInCallGuard(PickupSession session, List<String> expectedUnmet) {
		givenSession(session);

		SessionView view = getSessionService.getSession(
				new SessionQuery(user(GUARDIAN_ID, Role.GUARDIAN), SESSION_ID));

		assertThat(view.call().guard()).isEqualTo(new CallGuardView(false, expectedUnmet));
	}

	// 다른 가드가 섞이지 않도록 차량 정차 중(G6 통과)으로 준비한다
	static Stream<Arguments> guardUnmetSessions() {
		return Stream.of(
				Arguments.of(Named.of("장소 SELECTED · 경로 확정 전(G2 · G3)",
						beforeRouteConfirmed(VehiclePhase.STOPPED)),
						List.of("G2", "G3")),
				Arguments.of(Named.of("민준이 준비 시간을 5분 늘려 callNotBefore 17:05(G5)",
						afterRouteConfirmedWithExtendedPrep()), List.of("G5")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("principalsOutsideSession")
	@DisplayName("다른 가족의 세션을 조회하면 NOT_FOUND로 거절한다")
	void getSessionRejectsPrincipalOutsideSessionWithNotFound(AuthPrincipal principal) {
		givenSession(afterRouteConfirmedWithCalledChildren());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> getSessionService.getSession(new SessionQuery(principal, SESSION_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
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
				Named.of("세션에 없는 같은 가족 자녀", child(SIBLING_NOT_IN_SESSION_ID)));
	}

	static Stream<Named<AuthPrincipal>> vehicleAndGuardians() {
		return Stream.of(
				Named.of("차량", vehicle()),
				Named.of("GUARDIAN", user(GUARDIAN_ID, Role.GUARDIAN)),
				Named.of("CO_GUARDIAN", user(CO_GUARDIAN_ID, Role.CO_GUARDIAN)),
				Named.of("DELEGATE", user(DELEGATE_ID, Role.DELEGATE)));
	}

	// 세션은 sessionId로 읽기 전용 조회(load)한다. loadForUpdate · loadActiveBy*를 쓰면 빈 값이 나와 실패한다
	private void givenSession(PickupSession session) {
		when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
	}

	private void givenMembers(List<MemberInfo> members) {
		when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(members);
	}

	// 사진이 있는 멤버는 서명 URL, 서윤 · 박대리는 사진 URL이 없다(null)
	private static List<MemberInfo> membersWithProfileImages() {
		return List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1111-2222",
						GUARDIAN_PROFILE_IMAGE_URL, false),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444",
						CO_GUARDIAN_PROFILE_IMAGE_URL, false),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666", null, false),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", "010-7777-8888", MINJUN_PROFILE_IMAGE_URL, false),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", "010-9999-0000", null, false));
	}

	private static AuthPrincipal vehicle() {
		return new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	}

	private static AuthPrincipal child(long childUserId) {
		return user(childUserId, Role.CHILD);
	}

	private static AuthPrincipal user(long userId, Role role) {
		return new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
	}

	// SES-04 전: 장소는 SELECTED, 경로 확정 없음(routeVersion 0, routeSpotVersion null), 자녀는 호출 전이라 위치 없음
	private static PickupSession beforeRouteConfirmed() {
		return beforeRouteConfirmed(VehiclePhase.ROUTING);
	}

	private static PickupSession beforeRouteConfirmed(VehiclePhase vehiclePhase) {
		return session(vehiclePhase, SpotStatus.SELECTED, 3, 0, null,
				SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null),
				SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	// SES-04 뒤: 장소 CONFIRMED, routeVersion 1, routeSpotVersion = spotVersion. 두 자녀 모두 호출을 받아 위치가 있다
	private static PickupSession afterRouteConfirmedWithCalledChildren() {
		return session(VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, 6, 1, SPOT_VERSION,
				SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						MINJUN_LOCATION, MINJUN_LOCATED_AT, null, null),
				SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						SEOYUN_LOCATION, SEOYUN_LOCATED_AT, null, null));
	}

	// SES-04 뒤 두 자녀가 호출을 받은 세션에 마지막 차량 위치가 저장돼 있다. 자녀 기준(700m · 180초)을 보려고 ETA · 남은 거리를 받는다
	private static PickupSession sessionWithVehicleLocation(SessionStatus status, Instant etaAt,
			int remainingMeters) {
		List<SessionChild> children = List.of(
				SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						MINJUN_LOCATION, MINJUN_LOCATED_AT, null, null),
				SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						SEOYUN_LOCATION, SEOYUN_LOCATED_AT, null, null));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY_KST, status, VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, 6,
				SPOT_VERSION, 1, SPOT_VERSION, false, etaAt, remainingMeters, ETA_UPDATED_AT, null, null, null,
				null, null, null, null, null, VEHICLE_LOCATION, VEHICLE_LOCATED_AT, children);
	}

	// SES-04 뒤: 민준만 준비 완료(SIG-06)를 눌렀다. 두 자녀 모두 WAITING
	private static PickupSession afterRouteConfirmedWithReadyMinjun() {
		SessionChild minjun = SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null,
				null, null, null, null);
		minjun.markReady(MINJUN_READY_AT);
		return session(VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, 6, 1, SPOT_VERSION, minjun,
				SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	// SES-04 뒤: 민준은 호출을 확인해 이동 중(MEETING), 서윤은 아직 CALLED
	private static PickupSession afterRouteConfirmedWithMeetingChild() {
		return session(VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, 7, 1, SPOT_VERSION,
				SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null,
						MINJUN_LOCATION, MINJUN_LOCATED_AT, MINJUN_ACKNOWLEDGED_AT, null),
				SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.CALLED, 0, null,
						SEOYUN_LOCATION, SEOYUN_LOCATED_AT, null, null));
	}

	// SES-04 뒤: 장소 CONFIRMED · 경로 일치 · ETA 신선 · 차량 정차 중. 민준은 준비 시간을 5분 늘려 callNotBefore가 아직 오지 않았다
	private static PickupSession afterRouteConfirmedWithExtendedPrep() {
		return session(VehiclePhase.STOPPED, SpotStatus.CONFIRMED, 6, 1, SPOT_VERSION,
				SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.WAITING, 5, NOW.plusSeconds(300),
						null, null, null, null),
				SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.WAITING, 0, null, null, null,
						null, null));
	}

	// 민준이 타지 않아 예외가 열린 ON_HOLD 세션. incidentResolvedAt이 null이면 열린 예외
	private static PickupSession sessionWithIncident(Instant incidentResolvedAt) {
		List<SessionChild> children = List.of(
				SessionChild.restore(null, MINJUN_ID, MINJUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, null, null,
						null, null),
				SessionChild.restore(null, SEOYUN_ID, SEOYUN_SCHEDULE_ID, ChildPhase.MEETING, 0, null, null, null,
						null, null));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY_KST, SessionStatus.ON_HOLD, VehiclePhase.STOPPED, SpotStatus.CONFIRMED, 8,
				SPOT_VERSION, 1, SPOT_VERSION, false, ETA_AT, REMAINING_METERS, ETA_UPDATED_AT, null, null, null,
				IncidentType.CHILD_NOT_ON_BOARD, null, MINJUN_ID, INCIDENT_OPENED_AT, incidentResolvedAt, children);
	}

	private static PickupSession session(VehiclePhase vehiclePhase, SpotStatus spotStatus, int stateVersion,
			int routeVersion, Integer routeSpotVersion, SessionChild... children) {
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT,
				MEETING_POINT, TODAY_KST, SessionStatus.ACTIVE, vehiclePhase, spotStatus, stateVersion,
				SPOT_VERSION, routeVersion, routeSpotVersion, false, ETA_AT, REMAINING_METERS, ETA_UPDATED_AT, null,
				null, null, null, null, null, null, null, List.of(children));
	}

}

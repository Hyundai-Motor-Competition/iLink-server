package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ActiveSessionResult;
import com.example.pickupservice.pickup.application.port.in.CallGuardView;
import com.example.pickupservice.pickup.application.port.in.CallProgressView;
import com.example.pickupservice.pickup.application.port.in.CancelSessionCommand;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotCommand;
import com.example.pickupservice.pickup.application.port.in.ChildLocationView;
import com.example.pickupservice.pickup.application.port.in.CompletePickupCommand;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteCommand;
import com.example.pickupservice.pickup.application.port.in.ContactResult;
import com.example.pickupservice.pickup.application.port.in.HoldCommand;
import com.example.pickupservice.pickup.application.port.in.OpenIncidentCommand;
import com.example.pickupservice.pickup.application.port.in.RejectBoardingCommand;
import com.example.pickupservice.pickup.application.port.in.SessionBoardingView;
import com.example.pickupservice.pickup.application.port.in.SessionCallView;
import com.example.pickupservice.pickup.application.port.in.SessionChildView;
import com.example.pickupservice.pickup.application.port.in.SessionDriverView;
import com.example.pickupservice.pickup.application.port.in.SessionEtaView;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.application.port.in.SessionIncidentView;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.in.SessionRouteView;
import com.example.pickupservice.pickup.application.port.in.SessionSpotView;
import com.example.pickupservice.pickup.application.port.in.SessionStatusView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.in.StartPickupCommand;
import com.example.pickupservice.pickup.application.port.in.StartPickupResult;
import com.example.pickupservice.pickup.application.service.PickupErrors;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupViolation;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.domain.JoinCallMode;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.SESSION_ID;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.SESSION_VIEW_JSON;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.STATE_VERSION;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.sessionView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class PickupSessionControllerTest extends PickupWebTestSupport {

	private static final String ACTIVE_SESSION_PATH = "/v1/pickup-sessions/active";
	private static final String SESSIONS_PATH = "/v1/pickup-sessions";
	private static final String SESSION_PATH = "/v1/pickup-sessions/{sessionId}";
	private static final String MINJUN_PROFILE_IMAGE_URL = "https://storage.example.com/profiles/31.jpg?sig=c";
	private static final String GUARDIAN_PROFILE_IMAGE_URL = "https://storage.example.com/profiles/101.jpg?sig=a";
	private static final String ROUTE_PATH = "/v1/pickup-sessions/{sessionId}/route";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long SAVED_SPOT_ID = 21L;
	private static final long UNSAVED_SPOT_ID = 99L;
	private static final String NAV_ROUTE_ID = "nav-route-7f3a";
	private static final int STALE_STATE_VERSION = 14;
	private static final String ROUTE_BODY = """
			{"spotId":21,"navRouteId":"nav-route-7f3a","stateVersion":15}
			""";
	private static final String UNSAVED_SPOT_ROUTE_BODY = """
			{"spotId":99,"navRouteId":"nav-route-7f3a","stateVersion":15}
			""";
	private static final String STALE_ROUTE_BODY = """
			{"spotId":21,"navRouteId":"nav-route-7f3a","stateVersion":14}
			""";
	private static final String ROUTE_WITH_TOTAL_METERS_BODY = """
			{"spotId":21,"navRouteId":"nav-route-7f3a","stateVersion":15,"totalMeters":4200}
			""";
	private static final String NEGATIVE_TOTAL_METERS_ROUTE_BODY = """
			{"spotId":21,"navRouteId":"nav-route-7f3a","stateVersion":15,"totalMeters":-1}
			""";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final long CHILD_USER_ID = 131L;
	private static final long FIRST_SCHEDULE_ID = 801L;
	private static final long SECOND_SCHEDULE_ID = 802L;
	private static final LocalDate PICKUP_DATE = LocalDate.of(2026, 11, 5);
	private static final String START_BODY = """
			{"scheduleIds":[801,802],"date":"2026-11-05"}
			""";
	private static final String EMPTY_SCHEDULE_IDS_BODY = """
			{"scheduleIds":[],"date":"2026-11-05"}
			""";
	private static final AuthPrincipal VEHICLE = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	private static final AuthPrincipal DELEGATE = user(DELEGATE_USER_ID, Role.DELEGATE);
	private static final String SPOT_CHANGES_PATH = "/v1/pickup-sessions/{sessionId}/spot-changes";
	private static final String HOLD_PATH = "/v1/pickup-sessions/{sessionId}/hold";
	private static final long OTHER_SAVED_SPOT_ID = 22L;
	private static final String OTHER_SPOT_NAME = "해솔영어학원 후문 앞";
	private static final int SPOT_VERSION = 4;
	private static final String SPOT_CHANGE_BODY = """
			{"spotId":22,"stateVersion":15,"spotVersion":4}
			""";
	private static final String HOLD_REASON = "아이 수업이 늦게 끝나요";
	private static final String HOLD_BODY = """
			{"stateVersion":15,"reason":"아이 수업이 늦게 끝나요"}
			""";
	private static final String CANCELLATION_PATH = "/v1/pickup-sessions/{sessionId}/cancellation";
	private static final String CANCEL_REASON = "아이가 아파서 오늘은 쉬어요";
	private static final String CANCEL_BODY = """
			{"stateVersion":15,"reason":"아이가 아파서 오늘은 쉬어요"}
			""";
	private static final String EVENTS_PATH = "/v1/pickup-sessions/{sessionId}/events";
	private static final long AFTER_SEQ = 41L;
	private static final int EVENTS_LIMIT = 2;
	private static final String EVENT_PAGE_JSON = """
			{
			  "items": [
			    { "eventId": 9001, "seq": 42, "type": "CALL_SENT", "stateVersion": 16,
			      "occurredAt": "2026-11-05T18:09:02+09:00", "payload": { "callId": 3301, "trigger": "ETA_3MIN" } },
			    { "eventId": 9002, "seq": 43, "type": "CALL_DELIVERED", "stateVersion": 17,
			      "occurredAt": "2026-11-05T18:09:04+09:00", "payload": { "callId": 3301 } }
			  ],
			  "hasMore": true
			}
			""";
	private static final String CONTACTS_PATH = "/v1/pickup-sessions/{sessionId}/contacts";
	private static final String CONTACTS_JSON = """
			{
			  "items": [
			    { "name": "김지은", "role": "GUARDIAN", "phone": "010-1234-5678" },
			    { "name": "박선생", "role": "TEACHER", "phone": "010-9876-5432" },
			    { "name": "해솔영어학원", "role": "ACADEMY", "phone": "02-555-1234" }
			  ]
			}
			""";
	private static final String COMPLETION_PATH = "/v1/pickup-sessions/{sessionId}/completion";
	private static final long NOT_BOARDED_CHILD_USER_ID = 32L;
	private static final String COMPLETION_BODY_FORMAT = """
			{"mode":"%s","stateVersion":15,"notBoardedChildUserIds":[32]}
			""";
	private static final String COMPLETION_WITHOUT_NOT_BOARDED_BODY = """
			{"mode":"IMMEDIATE","stateVersion":15}
			""";
	private static final String BOARDING_REJECTION_PATH = "/v1/pickup-sessions/{sessionId}/boarding-rejection";
	private static final long REJECTED_CHILD_USER_ID = 32L;
	private static final String BOARDING_REJECTION_BODY = """
			{"childUserId":32,"stateVersion":15}
			""";
	private static final String BOARDING_REJECTION_WITHOUT_CHILD_BODY = """
			{"stateVersion":15}
			""";
	private static final Instant BOARDING_DETECTED_AT = Instant.parse("2026-11-05T09:13:00Z");
	private static final String INCIDENT_PATH = "/v1/pickup-sessions/{sessionId}/incident";
	private static final String INCIDENT_TYPE = "CHILD_NOT_ON_BOARD";
	private static final long INCIDENT_CHILD_USER_ID = 32L;
	private static final Instant INCIDENT_OPENED_AT = Instant.parse("2026-11-05T09:14:00Z");
	private static final String INCIDENT_BODY = """
			{"type":"CHILD_NOT_ON_BOARD","childUserId":32,"stateVersion":15}
			""";
	private static final String INCIDENT_TYPE_BODY_FORMAT = """
			{"type":%s,"childUserId":32,"stateVersion":15}
			""";
	private static final String INCIDENT_WITHOUT_CHILD_BODY = """
			{"type":"CHILD_NOT_ON_BOARD","stateVersion":15}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	static Stream<AuthPrincipal> allPrincipals() {
		return Stream.concat(Stream.of(VEHICLE), userPrincipals());
	}

	static Stream<AuthPrincipal> userPrincipals() {
		return Stream.of(
				user(GUARDIAN_USER_ID, Role.GUARDIAN),
				user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN),
				user(DELEGATE_USER_ID, Role.DELEGATE),
				user(CHILD_USER_ID, Role.CHILD));
	}

	static Stream<AuthPrincipal> spotChangers() {
		return Stream.of(
				VEHICLE,
				user(GUARDIAN_USER_ID, Role.GUARDIAN),
				user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN));
	}

	static Stream<AuthPrincipal> holders() {
		return Stream.of(
				VEHICLE,
				user(GUARDIAN_USER_ID, Role.GUARDIAN),
				user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN),
				DELEGATE);
	}

	static Stream<AuthPrincipal> cancellers() {
		return Stream.of(
				VEHICLE,
				user(GUARDIAN_USER_ID, Role.GUARDIAN),
				user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allPrincipals")
	@DisplayName("GET /v1/pickup-sessions/active는 진행 중 세션이 있으면 200과 sessionId · status · stateVersion을 돌려준다")
	void findActiveWithActiveSessionReturns200WithSessionIdStatusAndStateVersion(AuthPrincipal principal)
			throws Exception {
		given(findActiveSessionUseCase.findActive(any())).willReturn(Optional.of(
				new ActiveSessionResult(SESSION_ID, SessionStatus.ACTIVE, STATE_VERSION)));

		mockMvc.perform(findActiveRequest(principal))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"sessionId":1024,"status":"ACTIVE","stateVersion":15}
						""", JsonCompareMode.STRICT));

		then(findActiveSessionUseCase).should().findActive(principal);
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/active는 진행 중 세션이 없으면 204를 돌려준다")
	void findActiveWithoutActiveSessionReturns204() throws Exception {
		given(findActiveSessionUseCase.findActive(any())).willReturn(Optional.empty());

		mockMvc.perform(findActiveRequest(VEHICLE))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions는 새 세션이면 201과 SessionView를 돌려준다")
	void startNewSessionReturns201WithSessionView() throws Exception {
		given(startPickupUseCase.start(any())).willReturn(new StartPickupResult(true, sessionView()));

		mockMvc.perform(startRequest())
				.andExpect(status().isCreated())
				.andExpect(content().json(SESSION_VIEW_JSON, JsonCompareMode.STRICT));

		then(startPickupUseCase).should().start(new StartPickupCommand(
				VEHICLE, List.of(FIRST_SCHEDULE_ID, SECOND_SCHEDULE_ID), PICKUP_DATE));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions는 진행 중인 같은 세션이면 200을 돌려준다")
	void startSameActiveSessionReturns200WithSessionView() throws Exception {
		given(startPickupUseCase.start(any())).willReturn(new StartPickupResult(false, sessionView()));

		mockMvc.perform(startRequest())
				.andExpect(status().isOk())
				.andExpect(content().json(SESSION_VIEW_JSON, JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions가 오늘이 아니면 422 NOT_TODAY를 돌려준다")
	void startNotTodayReturns422NotToday() throws Exception {
		given(startPickupUseCase.start(any())).willThrow(PickupErrors.notToday());

		mockMvc.perform(startRequest())
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "NOT_TODAY"));

		then(startPickupUseCase).should().start(new StartPickupCommand(
				VEHICLE, List.of(FIRST_SCHEDULE_ID, SECOND_SCHEDULE_ID), PICKUP_DATE));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions에 빈 scheduleIds를 보내면 422 VALIDATION_FAILED를 돌려준다")
	void startWithEmptyScheduleIdsReturns422ValidationFailed() throws Exception {
		mockMvc.perform(startRequest(VEHICLE, EMPTY_SCHEDULE_IDS_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("scheduleIds")))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'scheduleIds')].reason",
						everyItem(not(emptyOrNullString()))));

		then(startPickupUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "본문 {0}")
	@ValueSource(strings = {
			"{\"scheduleIds\":[null],\"date\":\"2026-11-05\"}",
			"{\"scheduleIds\":[801,null],\"date\":\"2026-11-05\"}"
	})
	@DisplayName("POST /v1/pickup-sessions에 scheduleIds 원소가 null이면 422 VALIDATION_FAILED(fields scheduleIds)를 돌려주고 유스케이스를 부르지 않는다")
	void startWithNullScheduleIdElementReturns422ValidationFailedOnScheduleIds(String body) throws Exception {
		mockMvc.perform(startRequest(VEHICLE, body))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields").isNotEmpty())
				.andExpect(jsonPath("$.error.fields[*].field", everyItem(startsWith("scheduleIds"))))
				.andExpect(jsonPath("$.error.fields[*].reason", everyItem(not(emptyOrNullString()))));

		then(startPickupUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("userPrincipals")
	@DisplayName("POST /v1/pickup-sessions를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void startWithUserTokenReturns403Forbidden(AuthPrincipal principal) throws Exception {
		mockMvc.perform(startRequest(principal, START_BODY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(startPickupUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allPrincipals")
	@DisplayName("GET /v1/pickup-sessions/{sessionId}는 200과 SessionView 필드를 돌려준다")
	void getSessionReturns200WithSessionViewFields(AuthPrincipal principal) throws Exception {
		given(getSessionUseCase.getSession(any())).willReturn(sessionView());

		mockMvc.perform(get(SESSION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal)))
				.andExpect(status().isOk())
				.andExpect(content().json(SESSION_VIEW_JSON, JsonCompareMode.STRICT));

		then(getSessionUseCase).should().getSession(new SessionQuery(principal, SESSION_ID));
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}의 시각 필드는 +09:00으로 내려간다")
	void getSessionWritesEveryTimeFieldWithKstOffset() throws Exception {
		given(getSessionUseCase.getSession(any())).willReturn(sessionViewWithEveryTimeField());

		mockMvc.perform(get(SESSION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eta.stopEtaAt").value("2026-11-05T18:12:00+09:00"))
				.andExpect(jsonPath("$.eta.updatedAt").value("2026-11-05T18:12:05+09:00"))
				.andExpect(jsonPath("$.call.scheduledAt").value("2026-11-05T18:09:00+09:00"))
				.andExpect(jsonPath("$.call.progress.acceptedAt").value("2026-11-05T18:09:01+09:00"))
				.andExpect(jsonPath("$.call.progress.sentAt").value("2026-11-05T18:09:02+09:00"))
				.andExpect(jsonPath("$.call.progress.deliveredAt").value("2026-11-05T18:09:04+09:00"))
				.andExpect(jsonPath("$.call.progress.acknowledgedAt").value("2026-11-05T18:09:30+09:00"))
				.andExpect(jsonPath("$.children[0].callNotBefore").value("2026-11-05T18:10:00+09:00"))
				.andExpect(jsonPath("$.children[0].acknowledgedAt").value("2026-11-05T18:09:31+09:00"))
				.andExpect(jsonPath("$.children[0].boardedAt").value("2026-11-05T18:13:10+09:00"))
				.andExpect(jsonPath("$.children[0].location.at").value("2026-11-05T18:12:30+09:00"))
				.andExpect(jsonPath("$.boarding.detectedAt").value("2026-11-05T18:13:00+09:00"))
				.andExpect(jsonPath("$.boarding.cancelableUntil").value("2026-11-05T18:14:00+09:00"))
				.andExpect(jsonPath("$.incident.openedAt").value("2026-11-06T00:30:00+09:00"));
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}는 children[].profileImageUrl과 운전 중인 보호자 driver.name · driver.profileImageUrl을 돌려준다")
	void getSessionWritesChildProfileImageUrlAndDriver() throws Exception {
		given(getSessionUseCase.getSession(any())).willReturn(sessionViewWithProfileImagesAndDriver());

		mockMvc.perform(get(SESSION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.children[0].childUserId").value(31))
				.andExpect(jsonPath("$.children[0].profileImageUrl").value(MINJUN_PROFILE_IMAGE_URL))
				.andExpect(jsonPath("$.children[1].childUserId").value(32))
				.andExpect(jsonPath("$.children[1].profileImageUrl").value(nullValue()))
				.andExpect(jsonPath("$.children[1]", hasKey("profileImageUrl")))
				.andExpect(jsonPath("$.driver.name").value("김보호"))
				.andExpect(jsonPath("$.driver.profileImageUrl").value(GUARDIAN_PROFILE_IMAGE_URL));
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}는 driver가 없으면 driver를 null로 돌려준다")
	void getSessionWritesNullDriverWithoutGuardian() throws Exception {
		given(getSessionUseCase.getSession(any())).willReturn(sessionView());

		mockMvc.perform(get(SESSION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasKey("driver")))
				.andExpect(jsonPath("$.driver").value(nullValue()));
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/route는 200과 SessionView를 돌려준다")
	void confirmRouteReturns200WithSessionView() throws Exception {
		given(confirmRouteUseCase.confirmRoute(any())).willReturn(sessionView());

		mockMvc.perform(confirmRouteRequest(VEHICLE, ROUTE_BODY))
				.andExpect(status().isOk())
				.andExpect(content().json(SESSION_VIEW_JSON, JsonCompareMode.STRICT));

		then(confirmRouteUseCase).should().confirmRoute(new ConfirmRouteCommand(
				VEHICLE, SESSION_ID, SAVED_SPOT_ID, NAV_ROUTE_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/route 본문의 totalMeters 4200을 유스케이스 Command의 totalMeters에 담고 200을 돌려준다")
	void confirmRoutePassesTotalMetersToCommand() throws Exception {
		given(confirmRouteUseCase.confirmRoute(any())).willReturn(sessionView());

		mockMvc.perform(confirmRouteRequest(VEHICLE, ROUTE_WITH_TOTAL_METERS_BODY))
				.andExpect(status().isOk());

		ArgumentCaptor<ConfirmRouteCommand> captor = ArgumentCaptor.forClass(ConfirmRouteCommand.class);
		then(confirmRouteUseCase).should().confirmRoute(captor.capture());
		ConfirmRouteCommand command = captor.getValue();
		assertThat(command.totalMeters()).isEqualTo(4_200);
		assertThat(command.spotId()).isEqualTo(SAVED_SPOT_ID);
		assertThat(command.navRouteId()).isEqualTo(NAV_ROUTE_ID);
		assertThat(command.stateVersion()).isEqualTo(STATE_VERSION);
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/route 본문에 totalMeters가 없으면 Command의 totalMeters를 null로 담고 200을 돌려준다")
	void confirmRouteWithoutTotalMetersPassesNullTotalMeters() throws Exception {
		given(confirmRouteUseCase.confirmRoute(any())).willReturn(sessionView());

		mockMvc.perform(confirmRouteRequest(VEHICLE, ROUTE_BODY))
				.andExpect(status().isOk());

		ArgumentCaptor<ConfirmRouteCommand> captor = ArgumentCaptor.forClass(ConfirmRouteCommand.class);
		then(confirmRouteUseCase).should().confirmRoute(captor.capture());
		assertThat(captor.getValue().totalMeters()).isNull();
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/route의 totalMeters가 음수(-1)면 422 VALIDATION_FAILED(fields totalMeters)를 돌려주고 유스케이스를 부르지 않는다")
	void confirmRouteWithNegativeTotalMetersReturns422ValidationFailed() throws Exception {
		mockMvc.perform(confirmRouteRequest(VEHICLE, NEGATIVE_TOTAL_METERS_ROUTE_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields", hasSize(1)))
				.andExpect(jsonPath("$.error.fields[0].field").value("totalMeters"))
				.andExpect(jsonPath("$.error.fields[0].reason", not(emptyOrNullString())));

		then(confirmRouteUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}는 세션 뷰 route.totalMeters를 route.totalMeters로 돌려준다")
	void getSessionWritesRouteTotalMeters() throws Exception {
		given(getSessionUseCase.getSession(any())).willReturn(sessionViewWithRouteTotalMeters());

		mockMvc.perform(get(SESSION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.route.routeVersion").value(2))
				.andExpect(jsonPath("$.route.remainingMeters").value(4200))
				.andExpect(jsonPath("$.route.totalMeters").value(5800));
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/route가 저장하지 않은 장소면 422 SPOT_NOT_SAVED를 돌려준다")
	void confirmRouteWithUnsavedSpotReturns422SpotNotSaved() throws Exception {
		given(confirmRouteUseCase.confirmRoute(any())).willThrow(PickupErrors.spotNotSaved());

		mockMvc.perform(confirmRouteRequest(VEHICLE, UNSAVED_SPOT_ROUTE_BODY))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "SPOT_NOT_SAVED"));

		then(confirmRouteUseCase).should().confirmRoute(new ConfirmRouteCommand(
				VEHICLE, SESSION_ID, UNSAVED_SPOT_ID, NAV_ROUTE_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/route의 stateVersion이 다르면 409 STATE_VERSION_CONFLICT와 최신 세션 뷰를 돌려준다")
	void confirmRouteWithStaleStateVersionReturns409StateVersionConflictWithLatestSessionView() throws Exception {
		given(confirmRouteUseCase.confirmRoute(any()))
				.willThrow(PickupErrors.stateVersionConflict(sessionView()));
		Object latestSessionView = JsonPath.parse(SESSION_VIEW_JSON).read("$");

		mockMvc.perform(confirmRouteRequest(VEHICLE, STALE_ROUTE_BODY))
				.andExpect(commonError(HttpStatus.CONFLICT, "STATE_VERSION_CONFLICT"))
				.andExpect(jsonPath("$.session").value(latestSessionView));

		then(confirmRouteUseCase).should().confirmRoute(new ConfirmRouteCommand(
				VEHICLE, SESSION_ID, SAVED_SPOT_ID, NAV_ROUTE_ID, STALE_STATE_VERSION));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("userPrincipals")
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/route를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void confirmRouteWithUserTokenReturns403Forbidden(AuthPrincipal principal) throws Exception {
		mockMvc.perform(confirmRouteRequest(principal, ROUTE_BODY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(confirmRouteUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("spotChangers")
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/spot-changes는 200과 SessionView를 돌려준다")
	void changeSpotReturns200WithSessionView(AuthPrincipal principal) throws Exception {
		given(changeSpotUseCase.changeSpot(any())).willReturn(spotChangedSessionView());

		mockMvc.perform(changeSpotRequest(principal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.session").value("ACTIVE"))
				.andExpect(jsonPath("$.status.spot").value("SELECTED"))
				.andExpect(jsonPath("$.spot.spotId").value(OTHER_SAVED_SPOT_ID))
				.andExpect(jsonPath("$.spot.name").value(OTHER_SPOT_NAME))
				.andExpect(jsonPath("$.spot.spotVersion").value(SPOT_VERSION + 1))
				.andExpect(jsonPath("$.children[*].childUserId", contains(31, 32)));

		then(changeSpotUseCase).should().changeSpot(new ChangeSpotCommand(
				principal, SESSION_ID, OTHER_SAVED_SPOT_ID, STATE_VERSION, SPOT_VERSION));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/spot-changes가 자녀 이동 중이면 409 CHILD_ALREADY_MOVING을 돌려준다")
	void changeSpotWhileChildMovingReturns409ChildAlreadyMoving() throws Exception {
		given(changeSpotUseCase.changeSpot(any())).willThrow(PickupErrors.childAlreadyMoving());

		mockMvc.perform(changeSpotRequest(VEHICLE))
				.andExpect(commonError(HttpStatus.CONFLICT, "CHILD_ALREADY_MOVING"));

		then(changeSpotUseCase).should().changeSpot(new ChangeSpotCommand(
				VEHICLE, SESSION_ID, OTHER_SAVED_SPOT_ID, STATE_VERSION, SPOT_VERSION));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/spot-changes를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void changeSpotWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(changeSpotRequest(DELEGATE))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(changeSpotUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("holders")
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/hold는 200과 status.session ON_HOLD인 SessionView를 돌려준다")
	void holdReturns200WithOnHoldSessionView(AuthPrincipal principal) throws Exception {
		given(holdSessionUseCase.hold(any())).willReturn(onHoldSessionView());

		mockMvc.perform(holdRequest(principal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.session").value("ON_HOLD"))
				.andExpect(jsonPath("$.status.spot").value("CONFIRMED"))
				.andExpect(jsonPath("$.spot.spotId").value(SAVED_SPOT_ID));

		then(holdSessionUseCase).should().hold(
				new HoldCommand(principal, SESSION_ID, STATE_VERSION, HOLD_REASON));
		then(holdSessionUseCase).should(never()).releaseHold(any());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("holders")
	@DisplayName("DELETE /v1/pickup-sessions/{sessionId}/hold는 쿼리 stateVersion을 받아 200과 status.session ACTIVE인 SessionView를 돌려준다")
	void releaseHoldWithQueryStateVersionReturns200WithActiveSessionView(AuthPrincipal principal) throws Exception {
		given(holdSessionUseCase.releaseHold(any())).willReturn(releasedSessionView());

		mockMvc.perform(releaseHoldRequest(principal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.session").value("ACTIVE"))
				.andExpect(jsonPath("$.status.spot").value("CONFIRMED"))
				.andExpect(jsonPath("$.spot.spotId").value(SAVED_SPOT_ID));

		then(holdSessionUseCase).should().releaseHold(
				new HoldCommand(principal, SESSION_ID, STATE_VERSION, null));
		then(holdSessionUseCase).should(never()).hold(any());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cancellers")
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/cancellation은 200과 status.session CANCELLED인 SessionView를 돌려준다")
	void cancelReturns200WithCancelledSessionView(AuthPrincipal principal) throws Exception {
		given(cancelSessionUseCase.cancel(any())).willReturn(cancelledSessionView());

		mockMvc.perform(cancelRequest(principal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.session").value("CANCELLED"))
				.andExpect(jsonPath("$.children[*].childUserId", contains(31, 32)));

		then(cancelSessionUseCase).should().cancel(
				new CancelSessionCommand(principal, SESSION_ID, STATE_VERSION, CANCEL_REASON));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/cancellation이 끝난 세션이면 409 SESSION_ALREADY_FINISHED를 돌려준다")
	void cancelFinishedSessionReturns409SessionAlreadyFinished() throws Exception {
		given(cancelSessionUseCase.cancel(any())).willThrow(PickupErrors.sessionAlreadyFinished());

		mockMvc.perform(cancelRequest(VEHICLE))
				.andExpect(commonError(HttpStatus.CONFLICT, "SESSION_ALREADY_FINISHED"));

		then(cancelSessionUseCase).should().cancel(
				new CancelSessionCommand(VEHICLE, SESSION_ID, STATE_VERSION, CANCEL_REASON));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/cancellation을 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void cancelWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(cancelRequest(DELEGATE))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(cancelSessionUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allPrincipals")
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/events는 200과 items[] · hasMore를 돌려준다")
	void listEventsReturns200WithItemsAndHasMore(AuthPrincipal principal) throws Exception {
		given(listSessionEventsUseCase.listEvents(anyLong(), anyLong(), anyInt(), any()))
				.willReturn(new SessionEventPage(List.of(
						SessionEvent.restore(9001L, SESSION_ID, 42L, SessionEventType.CALL_SENT, 16,
								Instant.parse("2026-11-05T09:09:02Z"), Map.of("callId", 3301, "trigger", "ETA_3MIN")),
						SessionEvent.restore(9002L, SESSION_ID, 43L, SessionEventType.CALL_DELIVERED, 17,
								Instant.parse("2026-11-05T09:09:04Z"), Map.of("callId", 3301))),
						true));

		mockMvc.perform(listEventsRequest(principal, String.valueOf(EVENTS_LIMIT)))
				.andExpect(status().isOk())
				.andExpect(content().json(EVENT_PAGE_JSON, JsonCompareMode.STRICT));

		then(listSessionEventsUseCase).should().listEvents(SESSION_ID, AFTER_SEQ, EVENTS_LIMIT, principal);
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/events에 limit 101을 보내면 422 VALIDATION_FAILED를 돌려준다")
	void listEventsWithLimit101Returns422ValidationFailed() throws Exception {
		mockMvc.perform(listEventsRequest(VEHICLE, "101"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("limit")))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'limit')].reason",
						everyItem(not(emptyOrNullString()))));

		then(listSessionEventsUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allPrincipals")
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/contacts는 200과 items[](name · role · phone)를 돌려준다")
	void getContactsReturns200WithNameRoleAndPhoneItems(AuthPrincipal principal) throws Exception {
		given(getContactsUseCase.getContacts(any())).willReturn(List.of(
				new ContactResult("김지은", "GUARDIAN", "010-1234-5678"),
				new ContactResult("박선생", "TEACHER", "010-9876-5432"),
				new ContactResult("해솔영어학원", "ACADEMY", "02-555-1234")));

		mockMvc.perform(get(CONTACTS_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal)))
				.andExpect(status().isOk())
				.andExpect(content().json(CONTACTS_JSON, JsonCompareMode.STRICT));

		then(getContactsUseCase).should().getContacts(new SessionQuery(principal, SESSION_ID));
	}

	@ParameterizedTest(name = "mode {0}")
	@ValueSource(strings = {"IMMEDIATE", "MANUAL"})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/completion은 200과 status.session COMPLETED인 SessionView를 돌려준다")
	void completeReturns200WithCompletedSessionView(String mode) throws Exception {
		given(completePickupUseCase.complete(any())).willReturn(completedSessionView());

		mockMvc.perform(completionRequest(VEHICLE, COMPLETION_BODY_FORMAT.formatted(mode)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.session").value("COMPLETED"))
				.andExpect(jsonPath("$.children[*].childUserId", contains(31, 32)));

		then(completePickupUseCase).should().complete(new CompletePickupCommand(
				VEHICLE, SESSION_ID, mode, STATE_VERSION, List.of(NOT_BOARDED_CHILD_USER_ID)));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/completion이 MANUAL인데 정차 중이 아니면 422 VEHICLE_NOT_STOPPED를 돌려준다")
	void completeManualWhileNotStoppedReturns422VehicleNotStopped() throws Exception {
		given(completePickupUseCase.complete(any())).willThrow(PickupErrors.vehicleNotStopped());

		mockMvc.perform(completionRequest(VEHICLE, COMPLETION_BODY_FORMAT.formatted("MANUAL")))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "VEHICLE_NOT_STOPPED"));

		then(completePickupUseCase).should().complete(new CompletePickupCommand(
				VEHICLE, SESSION_ID, "MANUAL", STATE_VERSION, List.of(NOT_BOARDED_CHILD_USER_ID)));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/completion이 빠진 자녀가 있으면 422 CHILDREN_NOT_ALL_BOARDED를 돌려준다")
	void completeWithMissingChildReturns422ChildrenNotAllBoarded() throws Exception {
		given(completePickupUseCase.complete(any())).willThrow(PickupErrors.childrenNotAllBoarded());

		mockMvc.perform(completionRequest(VEHICLE, COMPLETION_WITHOUT_NOT_BOARDED_BODY))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "CHILDREN_NOT_ALL_BOARDED"));

		then(completePickupUseCase).should().complete(new CompletePickupCommand(
				VEHICLE, SESSION_ID, "IMMEDIATE", STATE_VERSION, List.of()));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/completion에 세션에 없는 childUserId를 notBoardedChildUserIds로 보내면 404 NOT_FOUND를 돌려준다")
	void completeWithChildNotInSessionReturns404NotFound() throws Exception {
		given(completePickupUseCase.complete(any()))
				.willThrow(PickupErrors.from(new PickupRuleException(PickupViolation.CHILD_NOT_IN_SESSION)));

		mockMvc.perform(completionRequest(VEHICLE,
						"{\"mode\":\"MANUAL\",\"stateVersion\":15,\"notBoardedChildUserIds\":[999]}"))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"));

		then(completePickupUseCase).should().complete(new CompletePickupCommand(
				VEHICLE, SESSION_ID, "MANUAL", STATE_VERSION, List.of(999L)));
	}

	@ParameterizedTest(name = "mode \"{0}\"")
	@ValueSource(strings = {"DOOR", ""})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/completion의 mode가 IMMEDIATE · MANUAL이 아니면 422 VALIDATION_FAILED를 돌려주고 유스케이스를 부르지 않는다")
	void completeWithUnknownModeReturns422ValidationFailed(String mode) throws Exception {
		mockMvc.perform(completionRequest(VEHICLE, COMPLETION_BODY_FORMAT.formatted(mode)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("mode")))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'mode')].reason",
						everyItem(not(emptyOrNullString()))));

		then(completePickupUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "본문 {0}")
	@ValueSource(strings = {
			"{\"mode\":\"MANUAL\",\"stateVersion\":15,\"notBoardedChildUserIds\":[null]}",
			"{\"mode\":\"MANUAL\",\"stateVersion\":15,\"notBoardedChildUserIds\":[131,null]}"
	})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/completion에 notBoardedChildUserIds 원소가 null이면 422 VALIDATION_FAILED(fields notBoardedChildUserIds)를 돌려주고 유스케이스를 부르지 않는다")
	void completeWithNullNotBoardedChildUserIdElementReturns422ValidationFailedOnNotBoardedChildUserIds(String body)
			throws Exception {
		given(completePickupUseCase.complete(any())).willReturn(completedSessionView());

		mockMvc.perform(completionRequest(VEHICLE, body))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields").isNotEmpty())
				.andExpect(jsonPath("$.error.fields[*].field", everyItem(startsWith("notBoardedChildUserIds"))))
				.andExpect(jsonPath("$.error.fields[*].reason", everyItem(not(emptyOrNullString()))));

		then(completePickupUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/boarding-rejection은 200과 SessionView를 돌려준다")
	void rejectBoardingReturns200WithSessionView() throws Exception {
		given(rejectBoardingUseCase.rejectBoarding(any())).willReturn(boardingRejectedSessionView());

		mockMvc.perform(boardingRejectionRequest(VEHICLE, BOARDING_REJECTION_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.session").value("ACTIVE"))
				.andExpect(jsonPath("$.status.vehicle").value("STOPPED"))
				.andExpect(jsonPath("$.boarding.detectedAt").value("2026-11-05T18:13:00+09:00"))
				.andExpect(jsonPath("$.boarding.cancelableUntil").value("2026-11-05T18:13:10+09:00"))
				.andExpect(jsonPath("$.boarding.door").value("REAR_RIGHT"))
				.andExpect(jsonPath("$.children[*].childUserId", contains(31, 32)))
				.andExpect(jsonPath("$.children[0].boardedAt").value("2026-11-05T18:13:00+09:00"))
				.andExpect(jsonPath("$.children[1].boardedAt").value(nullValue()));

		then(rejectBoardingUseCase).should().rejectBoarding(new RejectBoardingCommand(
				VEHICLE, SESSION_ID, REJECTED_CHILD_USER_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/boarding-rejection에 childUserId가 없으면 childUserId null로 유스케이스를 부른다")
	void rejectBoardingWithoutChildUserIdPassesNullChildUserId() throws Exception {
		given(rejectBoardingUseCase.rejectBoarding(any())).willReturn(boardingRejectedSessionView());

		mockMvc.perform(boardingRejectionRequest(VEHICLE, BOARDING_REJECTION_WITHOUT_CHILD_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID));

		then(rejectBoardingUseCase).should().rejectBoarding(new RejectBoardingCommand(
				VEHICLE, SESSION_ID, null, STATE_VERSION));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/boarding-rejection이 10초가 지났으면 409 REJECTION_WINDOW_CLOSED를 돌려준다")
	void rejectBoardingAfterWindowReturns409RejectionWindowClosed() throws Exception {
		given(rejectBoardingUseCase.rejectBoarding(any())).willThrow(PickupErrors.rejectionWindowClosed());

		mockMvc.perform(boardingRejectionRequest(VEHICLE, BOARDING_REJECTION_BODY))
				.andExpect(commonError(HttpStatus.CONFLICT, "REJECTION_WINDOW_CLOSED"));

		then(rejectBoardingUseCase).should().rejectBoarding(new RejectBoardingCommand(
				VEHICLE, SESSION_ID, REJECTED_CHILD_USER_ID, STATE_VERSION));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("userPrincipals")
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/boarding-rejection을 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void rejectBoardingWithUserTokenReturns403Forbidden(AuthPrincipal principal) throws Exception {
		mockMvc.perform(boardingRejectionRequest(principal, BOARDING_REJECTION_BODY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(rejectBoardingUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/boarding-rejection에 Idempotency-Key가 없으면 422 VALIDATION_FAILED를 돌려준다")
	void rejectBoardingWithoutIdempotencyKeyReturns422ValidationFailed() throws Exception {
		mockMvc.perform(post(BOARDING_REJECTION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE))
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOARDING_REJECTION_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields", hasSize(1)))
				.andExpect(jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER));

		then(rejectBoardingUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/incident는 200과 incident가 채워진 SessionView를 돌려준다")
	void openIncidentReturns200WithIncidentFilledSessionView() throws Exception {
		given(openIncidentUseCase.openIncident(any())).willReturn(incidentOpenedSessionView());

		mockMvc.perform(incidentRequest(VEHICLE, INCIDENT_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.session").value("ON_HOLD"))
				.andExpect(jsonPath("$.incident.type").value(INCIDENT_TYPE))
				.andExpect(jsonPath("$.incident.childUserId").value(INCIDENT_CHILD_USER_ID))
				.andExpect(jsonPath("$.incident.openedAt").value("2026-11-05T18:14:00+09:00"));

		then(openIncidentUseCase).should().openIncident(new OpenIncidentCommand(
				VEHICLE, SESSION_ID, INCIDENT_TYPE, INCIDENT_CHILD_USER_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/incident가 이미 열린 예외면 409 INCIDENT_ALREADY_OPEN을 돌려준다")
	void openIncidentWhileIncidentOpenReturns409IncidentAlreadyOpen() throws Exception {
		given(openIncidentUseCase.openIncident(any())).willThrow(PickupErrors.incidentAlreadyOpen());

		mockMvc.perform(incidentRequest(VEHICLE, INCIDENT_BODY))
				.andExpect(commonError(HttpStatus.CONFLICT, "INCIDENT_ALREADY_OPEN"));

		then(openIncidentUseCase).should().openIncident(new OpenIncidentCommand(
				VEHICLE, SESSION_ID, INCIDENT_TYPE, INCIDENT_CHILD_USER_ID, STATE_VERSION));
	}

	@ParameterizedTest(name = "type {0}")
	@ValueSource(strings = {"\"CHILD_LEFT_BEHIND\"", "\"\"", "null"})
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/incident의 type이 CHILD_NOT_ON_BOARD가 아니면 422 VALIDATION_FAILED(fields type)를 돌려주고 유스케이스를 부르지 않는다")
	void openIncidentWithUnknownTypeReturns422ValidationFailedOnType(String typeJson) throws Exception {
		mockMvc.perform(incidentRequest(VEHICLE, INCIDENT_TYPE_BODY_FORMAT.formatted(typeJson)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("type")))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'type')].reason",
						everyItem(not(emptyOrNullString()))));

		then(openIncidentUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/incident에 childUserId가 없으면 422 VALIDATION_FAILED(fields childUserId)를 돌려주고 유스케이스를 부르지 않는다")
	void openIncidentWithoutChildUserIdReturns422ValidationFailedOnChildUserId() throws Exception {
		mockMvc.perform(incidentRequest(VEHICLE, INCIDENT_WITHOUT_CHILD_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("childUserId")))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'childUserId')].reason",
						everyItem(not(emptyOrNullString()))));

		then(openIncidentUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("userPrincipals")
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/incident를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void openIncidentWithUserTokenReturns403Forbidden(AuthPrincipal principal) throws Exception {
		mockMvc.perform(incidentRequest(principal, INCIDENT_BODY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(openIncidentUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/incident에 Idempotency-Key가 없으면 422 VALIDATION_FAILED를 돌려준다")
	void openIncidentWithoutIdempotencyKeyReturns422ValidationFailed() throws Exception {
		mockMvc.perform(put(INCIDENT_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE))
						.contentType(MediaType.APPLICATION_JSON)
						.content(INCIDENT_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields", hasSize(1)))
				.andExpect(jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER));

		then(openIncidentUseCase).shouldHaveNoInteractions();
	}

	static Stream<Arguments> bodyRequestsWithoutStateVersion() {
		return Stream.of(
				Arguments.of(HttpMethod.PUT, ROUTE_PATH, """
						{"spotId":21,"navRouteId":"nav-route-7f3a"}
						"""),
				Arguments.of(HttpMethod.POST, SPOT_CHANGES_PATH, """
						{"spotId":22,"spotVersion":4}
						"""),
				Arguments.of(HttpMethod.PUT, HOLD_PATH, """
						{"reason":"아이 수업이 늦게 끝나요"}
						"""),
				Arguments.of(HttpMethod.POST, CANCELLATION_PATH, """
						{"reason":"아이가 아파서 오늘은 쉬어요"}
						"""),
				Arguments.of(HttpMethod.POST, COMPLETION_PATH, """
						{"mode":"IMMEDIATE"}
						"""),
				Arguments.of(HttpMethod.POST, BOARDING_REJECTION_PATH, """
						{"childUserId":32}
						"""),
				Arguments.of(HttpMethod.PUT, INCIDENT_PATH, """
						{"type":"CHILD_NOT_ON_BOARD","childUserId":32}
						"""));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("bodyRequestsWithoutStateVersion")
	@DisplayName("세션을 바꾸는 요청 본문에 stateVersion이 없으면 422 VALIDATION_FAILED(fields stateVersion)를 돌려준다")
	void bodyWithoutStateVersionReturns422ValidationFailedOnStateVersion(
			HttpMethod method, String path, String body) throws Exception {
		mockMvc.perform(request(method, path, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields", hasSize(1)))
				.andExpect(jsonPath("$.error.fields[0].field").value("stateVersion"));

		then(confirmRouteUseCase).shouldHaveNoInteractions();
		then(changeSpotUseCase).shouldHaveNoInteractions();
		then(holdSessionUseCase).shouldHaveNoInteractions();
		then(cancelSessionUseCase).shouldHaveNoInteractions();
		then(completePickupUseCase).shouldHaveNoInteractions();
		then(rejectBoardingUseCase).shouldHaveNoInteractions();
		then(openIncidentUseCase).shouldHaveNoInteractions();
	}

	static Stream<Arguments> bodyRequestsWithInvalidField() {
		String reason101 = "가".repeat(101);
		return Stream.of(
				Arguments.of("SES-02 date 없음", HttpMethod.POST, SESSIONS_PATH, """
						{"scheduleIds":[801,802]}
						""", "date"),
				Arguments.of("SES-04 spotId 없음", HttpMethod.PUT, ROUTE_PATH, """
						{"navRouteId":"nav-route-7f3a","stateVersion":15}
						""", "spotId"),
				Arguments.of("SES-05 spotId 없음", HttpMethod.POST, SPOT_CHANGES_PATH, """
						{"stateVersion":15,"spotVersion":4}
						""", "spotId"),
				Arguments.of("SES-05 spotVersion 없음", HttpMethod.POST, SPOT_CHANGES_PATH, """
						{"spotId":22,"stateVersion":15}
						""", "spotVersion"),
				Arguments.of("SES-06 reason 101자", HttpMethod.PUT, HOLD_PATH,
						"{\"stateVersion\":15,\"reason\":\"%s\"}".formatted(reason101), "reason"),
				Arguments.of("SES-07 reason 101자", HttpMethod.POST, CANCELLATION_PATH,
						"{\"stateVersion\":15,\"reason\":\"%s\"}".formatted(reason101), "reason"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("bodyRequestsWithInvalidField")
	@DisplayName("세션 요청의 필수 값(date · spotId)이 없거나 reason이 100자를 넘으면 422 VALIDATION_FAILED(fields 그 필드)를 돌려주고 유스케이스를 부르지 않는다")
	void bodyWithMissingRequiredFieldOrLongReasonReturns422ValidationFailedOnThatField(
			String caseName, HttpMethod method, String path, String body, String field) throws Exception {
		mockMvc.perform(request(method, path, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields", hasSize(1)))
				.andExpect(jsonPath("$.error.fields[0].field").value(field))
				.andExpect(jsonPath("$.error.fields[0].reason", not(emptyOrNullString())));

		then(startPickupUseCase).shouldHaveNoInteractions();
		then(confirmRouteUseCase).shouldHaveNoInteractions();
		then(changeSpotUseCase).shouldHaveNoInteractions();
		then(holdSessionUseCase).shouldHaveNoInteractions();
		then(cancelSessionUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder incidentRequest(AuthPrincipal principal, String body) {
		return put(INCIDENT_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	/** SES-13 뒤 모습: stateVersion +1, sessionStatus ON_HOLD, 정차 중, 서윤(32)에 대한 CHILD_NOT_ON_BOARD 예외가 열림. */
	private static SessionView incidentOpenedSessionView() {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				STATE_VERSION + 1,
				new SessionStatusView(SessionStatus.ON_HOLD, VehiclePhase.STOPPED, base.status().call(), base.status().spot()),
				base.spot(),
				base.eta(),
				base.call(),
				base.children(),
				base.route(),
				base.boarding(),
				new SessionIncidentView(INCIDENT_TYPE, INCIDENT_CHILD_USER_ID, INCIDENT_OPENED_AT));
	}

	private MockHttpServletRequestBuilder boardingRejectionRequest(AuthPrincipal principal, String body) {
		return post(BOARDING_REJECTION_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	/** SES-12 뒤 모습: stateVersion +1, 정차 중 탑승 인식(뒷문 오른쪽, 10초 창), 민준만 탑승 기록이 남고 서윤은 boardedAt이 비었다. */
	private static SessionView boardingRejectedSessionView() {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				STATE_VERSION + 1,
				new SessionStatusView(SessionStatus.ACTIVE, VehiclePhase.STOPPED, "ACKNOWLEDGED", base.status().spot()),
				base.spot(),
				base.eta(),
				base.call(),
				List.of(
						new SessionChildView(31L, "민준", ChildPhase.MEETING, 10, null, null, BOARDING_DETECTED_AT, null),
						new SessionChildView(32L, "서윤", ChildPhase.MEETING, 0, null, null, null, null)),
				base.route(),
				new SessionBoardingView(BOARDING_DETECTED_AT, BOARDING_DETECTED_AT.plusSeconds(10), "REAR_RIGHT"),
				base.incident());
	}

	private MockHttpServletRequestBuilder listEventsRequest(AuthPrincipal principal, String limit) {
		return get(EVENTS_PATH, SESSION_ID)
				.queryParam("afterSeq", String.valueOf(AFTER_SEQ))
				.queryParam("limit", limit)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal));
	}

	private MockHttpServletRequestBuilder completionRequest(AuthPrincipal principal, String body) {
		return post(COMPLETION_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	/** SES-11 뒤 모습: stateVersion +1, sessionStatus COMPLETED. 나머지는 고정 뷰 그대로. */
	private static SessionView completedSessionView() {
		return withSessionStatus(SessionStatus.COMPLETED);
	}

	private MockHttpServletRequestBuilder releaseHoldRequest(AuthPrincipal principal) {
		return delete(HOLD_PATH, SESSION_ID)
				.queryParam("stateVersion", String.valueOf(STATE_VERSION))
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
	}

	private MockHttpServletRequestBuilder cancelRequest(AuthPrincipal principal) {
		return post(CANCELLATION_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(CANCEL_BODY);
	}

	/** SES-06 DELETE 뒤 모습: stateVersion +1, sessionStatus ACTIVE. 나머지는 고정 뷰 그대로. */
	private static SessionView releasedSessionView() {
		return withSessionStatus(SessionStatus.ACTIVE);
	}

	/** SES-07 뒤 모습: stateVersion +1, sessionStatus CANCELLED. 나머지는 고정 뷰 그대로. */
	private static SessionView cancelledSessionView() {
		return withSessionStatus(SessionStatus.CANCELLED);
	}

	private static SessionView withSessionStatus(SessionStatus sessionStatus) {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				STATE_VERSION + 1,
				new SessionStatusView(sessionStatus, base.status().vehicle(), base.status().call(), base.status().spot()),
				base.spot(),
				base.eta(),
				base.call(),
				base.children(),
				base.route(),
				base.boarding(),
				base.incident());
	}

	private MockHttpServletRequestBuilder changeSpotRequest(AuthPrincipal principal) {
		return post(SPOT_CHANGES_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(SPOT_CHANGE_BODY);
	}

	private MockHttpServletRequestBuilder holdRequest(AuthPrincipal principal) {
		return put(HOLD_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(HOLD_BODY);
	}

	/** SES-05 뒤 모습: stateVersion +1, spotStatus SELECTED, 바뀐 장소 스냅샷, spotVersion +1. */
	private static SessionView spotChangedSessionView() {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				STATE_VERSION + 1,
				new SessionStatusView(SessionStatus.ACTIVE, base.status().vehicle(), base.status().call(), SpotStatus.SELECTED),
				new SessionSpotView(OTHER_SAVED_SPOT_ID, OTHER_SPOT_NAME, SPOT_VERSION + 1, false),
				base.eta(),
				base.call(),
				base.children(),
				base.route(),
				base.boarding(),
				base.incident());
	}

	/** SES-06 PUT 뒤 모습: stateVersion +1, sessionStatus ON_HOLD. 나머지는 고정 뷰 그대로. */
	private static SessionView onHoldSessionView() {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				STATE_VERSION + 1,
				new SessionStatusView(SessionStatus.ON_HOLD, base.status().vehicle(), base.status().call(), base.status().spot()),
				base.spot(),
				base.eta(),
				base.call(),
				base.children(),
				base.route(),
				base.boarding(),
				base.incident());
	}

	private MockHttpServletRequestBuilder confirmRouteRequest(AuthPrincipal principal, String body) {
		return put(ROUTE_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	/** 민준(31)은 사진 URL이 있고 서윤(32)은 없다. 운전 중인 보호자는 GUARDIAN 김보호. */
	private static SessionView sessionViewWithProfileImagesAndDriver() {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				base.stateVersion(),
				base.status(),
				base.spot(),
				base.eta(),
				base.call(),
				List.of(
						new SessionChildView(31L, "민준", ChildPhase.WAITING, 10,
								Instant.parse("2026-11-05T09:10:00Z"), null, null, null, null,
								MINJUN_PROFILE_IMAGE_URL),
						new SessionChildView(32L, "서윤", ChildPhase.WAITING, 0, null, null, null, null, null, null)),
				null,
				base.route(),
				base.boarding(),
				base.incident(),
				new SessionDriverView("김보호", GUARDIAN_PROFILE_IMAGE_URL));
	}

	/** 경로 확정 때 전체 거리 5800m, 남은 거리 4200m. */
	private static SessionView sessionViewWithRouteTotalMeters() {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				base.stateVersion(),
				base.status(),
				base.spot(),
				base.eta(),
				base.call(),
				base.children(),
				base.vehicleLocation(),
				SessionRouteView.of(2, 4200, 5800),
				base.boarding(),
				base.incident(),
				base.driver());
	}

	private static SessionView sessionViewWithEveryTimeField() {
		return new SessionView(
				SESSION_ID,
				STATE_VERSION,
				new SessionStatusView(SessionStatus.ACTIVE, VehiclePhase.STOPPED, "ACKNOWLEDGED", SpotStatus.CONFIRMED),
				new SessionSpotView(SAVED_SPOT_ID, "해솔영어학원 정문 앞", 4, true),
				new SessionEtaView(
						Instant.parse("2026-11-05T09:12:00Z"), 0, Instant.parse("2026-11-05T09:12:05Z"), false),
				new SessionCallView(
						JoinCallMode.AUTO, 3301L, false, Instant.parse("2026-11-05T09:09:00Z"),
						new CallGuardView(true, List.of()),
						new CallProgressView(
								Instant.parse("2026-11-05T09:09:01Z"),
								Instant.parse("2026-11-05T09:09:02Z"),
								Instant.parse("2026-11-05T09:09:04Z"),
								Instant.parse("2026-11-05T09:09:30Z"))),
				List.of(new SessionChildView(31L, "민준", ChildPhase.MEETING, 10,
						Instant.parse("2026-11-05T09:10:00Z"),
						Instant.parse("2026-11-05T09:09:31Z"),
						Instant.parse("2026-11-05T09:13:10Z"),
						new ChildLocationView(37.501301, 127.039901, Instant.parse("2026-11-05T09:12:30Z")))),
				new SessionRouteView(2, 0),
				new SessionBoardingView(
						Instant.parse("2026-11-05T09:13:00Z"), Instant.parse("2026-11-05T09:14:00Z"), "REAR_RIGHT"),
				new SessionIncidentView("CHILD_NOT_ON_BOARD", 31L, Instant.parse("2026-11-05T15:30:00Z")));
	}

	private MockHttpServletRequestBuilder findActiveRequest(AuthPrincipal principal) {
		return get(ACTIVE_SESSION_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal));
	}

	private MockHttpServletRequestBuilder startRequest() {
		return startRequest(VEHICLE, START_BODY);
	}

	private MockHttpServletRequestBuilder startRequest(AuthPrincipal principal, String body) {
		return post(SESSIONS_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private String bearerOf(AuthPrincipal principal) {
		if (principal.type() == PrincipalType.VEHICLE) {
			return bearer(accessTokenFixture.vehicle(principal.id(), principal.familyId()));
		}
		return bearer(accessTokenFixture.user(principal.id(), principal.familyId(), principal.role()));
	}

	private static AuthPrincipal user(long userId, Role role) {
		return new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
	}

}

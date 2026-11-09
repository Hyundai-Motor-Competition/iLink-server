package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.adapter.in.sse.SessionStreamRegistry;
import com.example.pickupservice.pickup.application.port.in.ActiveSessionResult;
import com.example.pickupservice.pickup.application.port.in.CorrectionView;
import com.example.pickupservice.pickup.application.port.in.DataSettingsResult;
import com.example.pickupservice.pickup.application.port.in.OutcomeView;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.STATE_VERSION;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.sessionView;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Idempotency-Key는 SES-02 · 04~07 · 11~13, SIG-03 · 05, CALL-01~04만 요구한다(#203).
 * SIG-01 · 02 · 04와 조회 · OUT API는 요구하지 않는다.
 */
@RequiredArgsConstructor
class PickupIdempotencyKeyTest extends PickupWebTestSupport {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CHILD_USER_ID = 31L;
	private static final long SESSION_ID = 1024L;
	private static final long CALL_ID = 3301L;
	private static final long CHILD_ID = 31L;
	private static final String EVENT_ID = "3f1c9b2e-8a4d-4c6e-9f10-2b7d5e8a1c34";

	private static final String SESSIONS_PATH = "/v1/pickup-sessions";
	private static final String SESSION_PATH = SESSIONS_PATH + "/{sessionId}";
	private static final String ACTIVE_PATH = SESSIONS_PATH + "/active";
	private static final String ROUTE_PATH = SESSION_PATH + "/route";
	private static final String SPOT_CHANGES_PATH = SESSION_PATH + "/spot-changes";
	private static final String HOLD_PATH = SESSION_PATH + "/hold";
	private static final String CANCELLATION_PATH = SESSION_PATH + "/cancellation";
	private static final String STREAM_PATH = SESSION_PATH + "/stream";
	private static final String EVENTS_PATH = SESSION_PATH + "/events";
	private static final String CONTACTS_PATH = SESSION_PATH + "/contacts";
	private static final String COMPLETION_PATH = SESSION_PATH + "/completion";
	private static final String BOARDING_REJECTION_PATH = SESSION_PATH + "/boarding-rejection";
	private static final String INCIDENT_PATH = SESSION_PATH + "/incident";
	private static final String TELEMETRY_PATH = SESSION_PATH + "/vehicle-telemetry";
	private static final String VEHICLE_EVENTS_PATH = SESSION_PATH + "/vehicle-events";
	private static final String PREP_EXTENSIONS_PATH = SESSION_PATH + "/prep-time-extensions";
	private static final String CHILD_LOCATIONS_PATH = SESSION_PATH + "/child-locations";
	private static final String HELP_REQUESTS_PATH = SESSION_PATH + "/help-requests";
	private static final String CALL_HOLD_PATH = SESSION_PATH + "/call-hold";
	private static final String CALLS_PATH = SESSION_PATH + "/calls";
	private static final String REVOCATION_PATH = "/v1/calls/{callId}/revocation";
	private static final String ACKNOWLEDGEMENT_PATH = "/v1/calls/{callId}/acknowledgement";
	private static final String HISTORY_PATH = "/v1/children/{childId}/pickup-history";
	private static final String OUTCOME_PATH = SESSION_PATH + "/outcome";
	private static final String CORRECTION_PATH = OUTCOME_PATH + "/correction";
	private static final String DATA_SETTINGS_PATH = "/v1/guardians/me/data-settings";

	private static final String START_BODY = """
			{"scheduleIds":[801,802],"date":"2026-11-05"}
			""";
	private static final String ROUTE_BODY = """
			{"spotId":21,"navRouteId":"nav-route-7f3a","stateVersion":15}
			""";
	private static final String SPOT_CHANGE_BODY = """
			{"spotId":22,"stateVersion":15,"spotVersion":4}
			""";
	private static final String HOLD_BODY = """
			{"stateVersion":15,"reason":"아이 수업이 늦게 끝나요"}
			""";
	private static final String CANCEL_BODY = """
			{"stateVersion":15,"reason":"아이가 아파서 오늘은 쉬어요"}
			""";
	private static final String COMPLETION_BODY = """
			{"mode":"IMMEDIATE","stateVersion":15}
			""";
	private static final String BOARDING_REJECTION_BODY = """
			{"childUserId":32,"stateVersion":15}
			""";
	private static final String INCIDENT_BODY = """
			{"type":"CHILD_NOT_ON_BOARD","childUserId":32,"stateVersion":15}
			""";
	private static final String STATE_VERSION_BODY = """
			{"stateVersion":15}
			""";
	private static final String REVOKE_BODY = """
			{"stateVersion":15,"reason":"아이가 아직 수업 중이에요"}
			""";
	private static final String PREP_EXTENSION_BODY = """
			{"minutes":10}
			""";
	private static final String HELP_REQUEST_BODY = """
			{"type":"NEED_HELP"}
			""";
	private static final String TELEMETRY_BODY = """
			{"observedAt":"2026-11-09T18:12:00+09:00","navEtaSeconds":0,"remainingMeters":0,\
			"location":{"lat":37.501234,"lng":127.039876}}
			""";
	private static final String VEHICLE_EVENT_BODY = """
			{"eventId":"%s","type":"STOPPED","occurredAt":"2026-11-09T18:20:30+09:00",\
			"location":{"lat":37.501234,"lng":127.039876}}
			""".formatted(EVENT_ID);
	private static final String CHILD_LOCATION_BODY = """
			{"at":"2026-11-09T18:20:30+09:00","lat":37.501234,"lng":127.039876}
			""";
	private static final String CORRECTION_BODY = """
			{"correctedStopAt":"2026-11-05T18:12:30+09:00","reasonCode":"WRONG_STOP_TIME",\
			"note":"정차 시각이 실제보다 1분 늦게 찍혔어요"}
			""";
	private static final String DATA_SETTINGS_BODY = """
			{"learningConsent":true}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;
	private final SessionStreamRegistry sessionStreamRegistry;

	@AfterEach
	void closeStreams() {
		sessionStreamRegistry.complete(SESSION_ID);
	}

	enum Caller {
		VEHICLE, GUARDIAN, CHILD
	}

	/**
	 * 한 API 호출. request는 Authorization · Idempotency-Key 헤더 없이 만든 요청이다.
	 */
	record ApiCall(String name, Caller caller, HttpStatus expectedStatus, Supplier<MockHttpServletRequestBuilder> request) {

		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<ApiCall> idempotencyKeyRequiredApis() {
		return Stream.of(
				required("SES-02 POST /v1/pickup-sessions", Caller.VEHICLE, () -> json(post(SESSIONS_PATH), START_BODY)),
				required("SES-04 PUT route", Caller.VEHICLE, () -> json(put(ROUTE_PATH, SESSION_ID), ROUTE_BODY)),
				required("SES-05 POST spot-changes", Caller.VEHICLE,
						() -> json(post(SPOT_CHANGES_PATH, SESSION_ID), SPOT_CHANGE_BODY)),
				required("SES-06 PUT hold", Caller.VEHICLE, () -> json(put(HOLD_PATH, SESSION_ID), HOLD_BODY)),
				required("SES-06 DELETE hold", Caller.VEHICLE,
						() -> delete(HOLD_PATH, SESSION_ID).queryParam("stateVersion", "15")),
				required("SES-07 POST cancellation", Caller.VEHICLE,
						() -> json(post(CANCELLATION_PATH, SESSION_ID), CANCEL_BODY)),
				required("SES-11 POST completion", Caller.VEHICLE,
						() -> json(post(COMPLETION_PATH, SESSION_ID), COMPLETION_BODY)),
				required("SES-12 POST boarding-rejection", Caller.VEHICLE,
						() -> json(post(BOARDING_REJECTION_PATH, SESSION_ID), BOARDING_REJECTION_BODY)),
				required("SES-13 PUT incident", Caller.VEHICLE,
						() -> json(put(INCIDENT_PATH, SESSION_ID), INCIDENT_BODY)),
				required("SIG-03 POST prep-time-extensions", Caller.CHILD,
						() -> json(post(PREP_EXTENSIONS_PATH, SESSION_ID), PREP_EXTENSION_BODY)),
				required("SIG-05 POST help-requests", Caller.CHILD,
						() -> json(post(HELP_REQUESTS_PATH, SESSION_ID), HELP_REQUEST_BODY)),
				required("CALL-01 PUT call-hold", Caller.VEHICLE,
						() -> json(put(CALL_HOLD_PATH, SESSION_ID), STATE_VERSION_BODY)),
				required("CALL-01 DELETE call-hold", Caller.VEHICLE,
						() -> delete(CALL_HOLD_PATH, SESSION_ID).queryParam("stateVersion", "15")),
				required("CALL-02 POST calls", Caller.VEHICLE,
						() -> json(post(CALLS_PATH, SESSION_ID), STATE_VERSION_BODY)),
				required("CALL-03 POST revocation", Caller.VEHICLE,
						() -> json(post(REVOCATION_PATH, CALL_ID), REVOKE_BODY)),
				required("CALL-04 POST acknowledgement", Caller.CHILD, () -> post(ACKNOWLEDGEMENT_PATH, CALL_ID)));
	}

	static Stream<ApiCall> idempotencyKeyNotRequiredApis() {
		return Stream.of(
				notRequired("SES-01 GET active", Caller.VEHICLE, HttpStatus.OK, () -> get(ACTIVE_PATH)),
				notRequired("SES-03 GET session", Caller.VEHICLE, HttpStatus.OK, () -> get(SESSION_PATH, SESSION_ID)),
				notRequired("SES-08 GET stream", Caller.VEHICLE, HttpStatus.OK,
						() -> get(STREAM_PATH, SESSION_ID).accept(MediaType.TEXT_EVENT_STREAM)),
				notRequired("SES-09 GET events", Caller.VEHICLE, HttpStatus.OK,
						() -> get(EVENTS_PATH, SESSION_ID).queryParam("afterSeq", "0")),
				notRequired("SES-10 GET contacts", Caller.VEHICLE, HttpStatus.OK, () -> get(CONTACTS_PATH, SESSION_ID)),
				notRequired("SIG-01 POST vehicle-telemetry", Caller.VEHICLE, HttpStatus.ACCEPTED,
						() -> json(post(TELEMETRY_PATH, SESSION_ID), TELEMETRY_BODY)),
				notRequired("SIG-02 POST vehicle-events", Caller.VEHICLE, HttpStatus.ACCEPTED,
						() -> json(post(VEHICLE_EVENTS_PATH, SESSION_ID), VEHICLE_EVENT_BODY)),
				notRequired("SIG-04 POST child-locations", Caller.CHILD, HttpStatus.ACCEPTED,
						() -> json(post(CHILD_LOCATIONS_PATH, SESSION_ID), CHILD_LOCATION_BODY)),
				notRequired("OUT-01 GET pickup-history", Caller.GUARDIAN, HttpStatus.OK,
						() -> get(HISTORY_PATH, CHILD_ID).queryParam("month", "2026-11")),
				notRequired("OUT-02 GET outcome", Caller.GUARDIAN, HttpStatus.OK, () -> get(OUTCOME_PATH, SESSION_ID)),
				notRequired("OUT-03 PUT outcome/correction", Caller.GUARDIAN, HttpStatus.OK,
						() -> json(put(CORRECTION_PATH, SESSION_ID), CORRECTION_BODY)),
				notRequired("OUT-04 PUT data-settings", Caller.GUARDIAN, HttpStatus.OK,
						() -> json(put(DATA_SETTINGS_PATH), DATA_SETTINGS_BODY)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("idempotencyKeyRequiredApis")
	@DisplayName("Idempotency-Key가 필요한 API를 헤더 없이 부르면 422 VALIDATION_FAILED(field Idempotency-Key)를 돌려주고 유스케이스를 부르지 않는다")
	void requiredApiWithoutIdempotencyKeyReturns422AndCallsNoUseCase(ApiCall api) throws Exception {
		mockMvc.perform(withBearer(api))
				.andExpect(missingIdempotencyKey());

		then(startPickupUseCase).shouldHaveNoInteractions();
		then(confirmRouteUseCase).shouldHaveNoInteractions();
		then(changeSpotUseCase).shouldHaveNoInteractions();
		then(holdSessionUseCase).shouldHaveNoInteractions();
		then(cancelSessionUseCase).shouldHaveNoInteractions();
		then(completePickupUseCase).shouldHaveNoInteractions();
		then(rejectBoardingUseCase).shouldHaveNoInteractions();
		then(openIncidentUseCase).shouldHaveNoInteractions();
		then(extendPrepTimeUseCase).shouldHaveNoInteractions();
		then(requestHelpUseCase).shouldHaveNoInteractions();
		then(holdCallUseCase).shouldHaveNoInteractions();
		then(sendCallUseCase).shouldHaveNoInteractions();
		then(revokeCallUseCase).shouldHaveNoInteractions();
		then(acknowledgeCallUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("idempotencyKeyNotRequiredApis")
	@DisplayName("Idempotency-Key가 필요 없는 API는 헤더 없이도 성공 상태를 돌려준다")
	void notRequiredApiWithoutIdempotencyKeySucceeds(ApiCall api) throws Exception {
		stubSuccessfulUseCases();

		mockMvc.perform(withBearer(api))
				.andExpect(status().is(api.expectedStatus().value()));
	}

	private void stubSuccessfulUseCases() {
		given(findActiveSessionUseCase.findActive(any()))
				.willReturn(java.util.Optional.of(new ActiveSessionResult(SESSION_ID, SessionStatus.ACTIVE, STATE_VERSION)));
		given(getSessionUseCase.getSession(any())).willReturn(sessionView());
		given(listSessionEventsUseCase.listEvents(anyLong(), anyLong(), anyInt(), any()))
				.willReturn(new SessionEventPage(List.of(), false));
		given(getContactsUseCase.getContacts(any())).willReturn(List.of());
		given(getPickupHistoryUseCase.history(any())).willReturn(List.of());
		OutcomeView outcome = new OutcomeView(SESSION_ID, Instant.parse("2026-11-05T09:20:00Z"), "해솔영어학원 정문 앞",
				"DOOR", Instant.parse("2026-11-05T09:12:00Z"), Instant.parse("2026-11-05T09:13:30Z"), 90L,
				new CorrectionView(null, null));
		given(getOutcomeUseCase.getOutcome(any())).willReturn(outcome);
		given(correctOutcomeUseCase.correct(any())).willReturn(outcome);
		given(updateDataSettingsUseCase.updateDataSettings(any())).willReturn(new DataSettingsResult(true, 90));
	}

	private MockHttpServletRequestBuilder withBearer(ApiCall api) {
		return api.request().get().header(HttpHeaders.AUTHORIZATION, bearerOf(api.caller()));
	}

	private String bearerOf(Caller caller) {
		return switch (caller) {
			case VEHICLE -> bearer(accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID));
			case GUARDIAN -> bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN));
			case CHILD -> bearer(accessTokenFixture.user(CHILD_USER_ID, FAMILY_ID, Role.CHILD));
		};
	}

	private static ApiCall required(String name, Caller caller, Supplier<MockHttpServletRequestBuilder> request) {
		return new ApiCall(name, caller, HttpStatus.UNPROCESSABLE_CONTENT, request);
	}

	private static ApiCall notRequired(String name, Caller caller, HttpStatus expected,
			Supplier<MockHttpServletRequestBuilder> request) {
		return new ApiCall(name, caller, expected, request);
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static ResultMatcher missingIdempotencyKey() {
		return result -> {
			status().isUnprocessableContent().match(result);
			commonErrorKeys().match(result);
			jsonPath("$.error.code").value("VALIDATION_FAILED").match(result);
			jsonPath("$.error.fields", hasSize(1)).match(result);
			jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER).match(result);
		};
	}

}

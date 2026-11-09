package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ChildLocationCommand;
import com.example.pickupservice.pickup.application.port.in.ExtendPrepTimeCommand;
import com.example.pickupservice.pickup.application.port.in.HelpRequestCommand;
import com.example.pickupservice.pickup.application.port.in.HelpRequestResult;
import com.example.pickupservice.pickup.application.port.in.PrepExtensionResult;
import com.example.pickupservice.pickup.application.port.in.TelemetryCommand;
import com.example.pickupservice.pickup.application.port.in.VehicleEventCommand;
import com.example.pickupservice.pickup.application.service.PickupErrors;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SIG-01 · 02. 권한은 차량 앱만이고 Idempotency-Key는 요구하지 않으므로(#203) 요청에 그 헤더를 싣지 않는다.
 * 202는 본문이 없다.
 * SIG-03 · 04 · 05. 권한은 자녀(CHILD) 앱만이다. Idempotency-Key는 SIG-03 · 05만 요구하고 SIG-04는 요구하지 않는다.
 */
@RequiredArgsConstructor
class PickupSignalControllerTest extends PickupWebTestSupport {

	private static final String TELEMETRY_PATH = "/v1/pickup-sessions/{sessionId}/vehicle-telemetry";
	private static final String VEHICLE_EVENTS_PATH = "/v1/pickup-sessions/{sessionId}/vehicle-events";
	private static final String PREP_EXTENSIONS_PATH = "/v1/pickup-sessions/{sessionId}/prep-time-extensions";
	private static final String CHILD_LOCATIONS_PATH = "/v1/pickup-sessions/{sessionId}/child-locations";
	private static final String HELP_REQUESTS_PATH = "/v1/pickup-sessions/{sessionId}/help-requests";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long CHILD_USER_ID = 31L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final AuthPrincipal CHILD = new AuthPrincipal(PrincipalType.USER, CHILD_USER_ID, 12L, Role.CHILD);
	private static final Instant CHILD_LOCATION_AT = Instant.parse("2026-11-09T09:20:30Z");
	private static final String CHILD_LOCATION_BODY_WITH_ACCURACY = """
			{"at":"2026-11-09T18:20:30+09:00","lat":37.501234,"lng":127.039876,"accuracyM":12.5}
			""";
	private static final String CHILD_LOCATION_BODY_WITHOUT_ACCURACY = """
			{"at":"2026-11-09T18:20:30+09:00","lat":37.501234,"lng":127.039876}
			""";
	private static final long SESSION_ID = 501L;
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final AuthPrincipal VEHICLE = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final Instant OBSERVED_AT = Instant.parse("2026-11-09T09:12:00Z");
	private static final Instant OCCURRED_AT = Instant.parse("2026-11-09T09:20:30Z");
	private static final String EVENT_ID = "3f1c9b2e-8a4d-4c6e-9f10-2b7d5e8a1c34";
	private static final String FULL_TELEMETRY_BODY = """
			{"observedAt":"2026-11-09T18:12:00+09:00","navEtaSeconds":300,"remainingMeters":1200,\
			"location":{"lat":37.501234,"lng":127.039876},"headingDeg":90,"speedKmh":32.5,"gear":"D"}
			""";
	private static final String REQUIRED_ONLY_TELEMETRY_BODY = """
			{"observedAt":"2026-11-09T18:12:00+09:00","navEtaSeconds":0,"remainingMeters":0,\
			"location":{"lat":37.501234,"lng":127.039876}}
			""";
	private static final String NEGATIVE_NAV_ETA_TELEMETRY_BODY = """
			{"observedAt":"2026-11-09T18:12:00+09:00","navEtaSeconds":-1,"remainingMeters":1200,\
			"location":{"lat":37.501234,"lng":127.039876}}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@ParameterizedTest(name = "{0}")
	@MethodSource("telemetryBodies")
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry는 202를 돌려준다")
	void receiveTelemetryReturns202(String caseName, String body, TelemetryCommand expectedCommand) throws Exception {
		mockMvc.perform(telemetryRequest(body))
				.andExpect(status().isAccepted())
				.andExpect(content().string(""));

		then(receiveTelemetryUseCase).should().receive(expectedCommand);
	}

	static Stream<Arguments> telemetryBodies() {
		return Stream.of(
				Arguments.of("선택 필드 모두", FULL_TELEMETRY_BODY,
						new TelemetryCommand(VEHICLE, SESSION_ID, OBSERVED_AT, 300, 1200, LOCATION, 90, 32.5, "D")),
				Arguments.of("필수 필드만, 0", REQUIRED_ONLY_TELEMETRY_BODY,
						new TelemetryCommand(VEHICLE, SESSION_ID, OBSERVED_AT, 0, 0, LOCATION, null, null, null)));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-telemetry에 음수 navEtaSeconds를 보내면 422 VALIDATION_FAILED를 돌려준다")
	void receiveTelemetryWithNegativeNavEtaSecondsReturns422ValidationFailed() throws Exception {
		mockMvc.perform(telemetryRequest(NEGATIVE_NAV_ETA_TELEMETRY_BODY))
				.andExpect(validationFailedOn("navEtaSeconds"));

		then(receiveTelemetryUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "type {0}, door {1}")
	@CsvSource({
			"STOPPED,",
			"DEPARTED,",
			"STOP_FAILED,",
			"DOOR_CYCLE, FRONT_RIGHT",
			"DOOR_CYCLE, REAR_LEFT",
			"DOOR_CYCLE, REAR_RIGHT"
	})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-events는 202를 돌려준다")
	void receiveVehicleEventReturns202(String type, String door) throws Exception {
		mockMvc.perform(vehicleEventRequest(vehicleEventBody(type, door)))
				.andExpect(status().isAccepted())
				.andExpect(content().string(""));

		then(receiveVehicleEventUseCase).should().receive(
				new VehicleEventCommand(VEHICLE, SESSION_ID, EVENT_ID, type, OCCURRED_AT, LOCATION, door));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-events에 door 없는 DOOR_CYCLE을 보내면 422 VALIDATION_FAILED를 돌려준다")
	void receiveDoorCycleWithoutDoorReturns422ValidationFailed() throws Exception {
		mockMvc.perform(vehicleEventRequest(vehicleEventBody("DOOR_CYCLE", null)))
				.andExpect(validationFailedOn("door"));

		then(receiveVehicleEventUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "door {0}")
	@ValueSource(strings = {"FRONT_LEFT", "TRUNK", "rear_left"})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-events에 운전석(FRONT_LEFT)이나 FRONT_RIGHT · REAR_LEFT · REAR_RIGHT 밖의 door로 DOOR_CYCLE을 보내면 422 VALIDATION_FAILED를 돌려준다")
	void receiveDoorCycleWithDriverOrUnknownDoorReturns422ValidationFailed(String door) throws Exception {
		mockMvc.perform(vehicleEventRequest(vehicleEventBody("DOOR_CYCLE", door)))
				.andExpect(validationFailedOn("door"));

		then(receiveVehicleEventUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"eventId", "type", "occurredAt"})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-events에 필수 값(eventId · type · occurredAt)이 없으면 422 VALIDATION_FAILED(fields 그 필드)를 돌려주고 유스케이스를 부르지 않는다")
	void receiveVehicleEventWithoutRequiredFieldReturns422ValidationFailedOnThatField(String missingField)
			throws Exception {
		mockMvc.perform(vehicleEventRequest(vehicleEventBodyWithout(missingField)))
				.andExpect(validationFailedOn(missingField))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value(missingField));

		then(receiveVehicleEventUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "eventId {0}")
	@ValueSource(strings = {"not-a-uuid", "12345", "3f1c9b2e-8a4d-4c6e-9f10-2b7d5e8a1c3",
			"3f1c9b2e-8a4d-4c6e-9f10-2b7d5e8a1c34-extra"})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-events에 UUID가 아닌 eventId를 보내면 422 VALIDATION_FAILED(fields[0].field eventId)를 돌려주고 유스케이스를 부르지 않는다")
	void receiveVehicleEventWithNonUuidEventIdReturns422ValidationFailedOnEventId(String eventId) throws Exception {
		mockMvc.perform(vehicleEventRequest(vehicleEventBody(eventId, "STOPPED", null)))
				.andExpect(validationFailedOn("eventId"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("eventId"));

		then(receiveVehicleEventUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "type {0}")
	@ValueSource(strings = {"ARRIVED", "stopped", "DOOR_OPENED", "UNKNOWN"})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/vehicle-events에 STOPPED · DEPARTED · STOP_FAILED · DOOR_CYCLE 밖의 type을 보내면 422 VALIDATION_FAILED(fields[0].field type)를 돌려주고 유스케이스를 부르지 않는다")
	void receiveVehicleEventWithUnknownTypeReturns422ValidationFailedOnType(String type) throws Exception {
		mockMvc.perform(vehicleEventRequest(vehicleEventBody(EVENT_ID, type, null)))
				.andExpect(validationFailedOn("type"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("type"));

		then(receiveVehicleEventUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "type {0}")
	@ValueSource(strings = {"SOS", "need_help", "CALL_POLICE", "STOPPED"})
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/help-requests에 NEED_HELP · CANT_FIND_CAR · CONTACT_PARENT 밖의 type을 보내면 422 VALIDATION_FAILED(fields[0].field type)를 돌려주고 유스케이스를 부르지 않는다")
	void requestHelpWithUnknownTypeReturns422ValidationFailedOnType(String type) throws Exception {
		mockMvc.perform(helpRequest(childBearer(), type))
				.andExpect(validationFailedOn("type"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("type"));

		then(requestHelpUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/prep-time-extensions는 200과 extraPrepMinutes · callNotBefore를 돌려준다")
	void extendPrepTimeReturns200WithExtraPrepMinutesAndCallNotBefore() throws Exception {
		given(extendPrepTimeUseCase.extend(any()))
				.willReturn(new PrepExtensionResult(10, Instant.parse("2026-11-09T09:15:00Z")));

		mockMvc.perform(prepExtensionRequest(childBearer(), "{\"minutes\":10}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.extraPrepMinutes").value(10))
				.andExpect(jsonPath("$.callNotBefore").value("2026-11-09T18:15:00+09:00"));

		then(extendPrepTimeUseCase).should().extend(new ExtendPrepTimeCommand(CHILD, SESSION_ID, 10));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/prep-time-extensions에 minutes 7을 보내면 422 VALIDATION_FAILED를 돌려준다")
	void extendPrepTimeWithMinutes7Returns422ValidationFailed() throws Exception {
		mockMvc.perform(prepExtensionRequest(childBearer(), "{\"minutes\":7}"))
				.andExpect(validationFailedOn("minutes"));

		then(extendPrepTimeUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/prep-time-extensions가 누적 15분을 넘으면 422 PREP_EXTENSION_LIMIT을 돌려준다")
	void extendPrepTimeOverTotalLimitReturns422PrepExtensionLimit() throws Exception {
		given(extendPrepTimeUseCase.extend(any())).willThrow(PickupErrors.prepExtensionLimit());

		mockMvc.perform(prepExtensionRequest(childBearer(), "{\"minutes\":10}"))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "PREP_EXTENSION_LIMIT"));

		then(extendPrepTimeUseCase).should().extend(new ExtendPrepTimeCommand(CHILD, SESSION_ID, 10));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/prep-time-extensions를 GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void extendPrepTimeWithGuardianTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(prepExtensionRequest(guardianBearer(), "{\"minutes\":5}"))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(extendPrepTimeUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("childLocationBodies")
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/child-locations는 202를 돌려준다")
	void reportChildLocationReturns202(String caseName, String body, Double accuracyM) throws Exception {
		mockMvc.perform(childLocationRequest(body))
				.andExpect(status().isAccepted())
				.andExpect(content().string(""));

		then(reportChildLocationUseCase).should().report(
				new ChildLocationCommand(CHILD, SESSION_ID, CHILD_LOCATION_AT, 37.501234, 127.039876, accuracyM));
	}

	static Stream<Arguments> childLocationBodies() {
		return Stream.of(
				Arguments.of("accuracyM 있음", CHILD_LOCATION_BODY_WITH_ACCURACY, 12.5),
				Arguments.of("accuracyM 없음", CHILD_LOCATION_BODY_WITHOUT_ACCURACY, null));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/child-locations가 동의 없으면 403 LOCATION_CONSENT_REQUIRED를 돌려준다")
	void reportChildLocationWithoutConsentReturns403LocationConsentRequired() throws Exception {
		willThrow(PickupErrors.locationConsentRequired()).given(reportChildLocationUseCase).report(any());

		mockMvc.perform(childLocationRequest(CHILD_LOCATION_BODY_WITH_ACCURACY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "LOCATION_CONSENT_REQUIRED"));

		then(reportChildLocationUseCase).should().report(
				new ChildLocationCommand(CHILD, SESSION_ID, CHILD_LOCATION_AT, 37.501234, 127.039876, 12.5));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/help-requests는 201과 nextAllowedAt을 돌려준다")
	void requestHelpReturns201WithNextAllowedAt() throws Exception {
		given(requestHelpUseCase.request(any()))
				.willReturn(new HelpRequestResult(Instant.parse("2026-11-09T09:21:30Z")));

		mockMvc.perform(helpRequest(childBearer()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nextAllowedAt").value("2026-11-09T18:21:30+09:00"));

		then(requestHelpUseCase).should().request(new HelpRequestCommand(CHILD, SESSION_ID, "NEED_HELP"));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/help-requests가 1분 안 재요청이면 429 HELP_REQUEST_TOO_SOON과 nextAllowedAt을 돌려준다")
	void requestHelpWithinOneMinuteReturns429HelpRequestTooSoonWithNextAllowedAt() throws Exception {
		given(requestHelpUseCase.request(any()))
				.willThrow(PickupErrors.helpRequestTooSoon(Instant.parse("2026-11-09T09:21:30Z")));

		mockMvc.perform(helpRequest(childBearer()))
				.andExpect(commonError(HttpStatus.TOO_MANY_REQUESTS, "HELP_REQUEST_TOO_SOON"))
				.andExpect(jsonPath("$.error.nextAllowedAt").value("2026-11-09T18:21:30+09:00"));

		then(requestHelpUseCase).should().request(new HelpRequestCommand(CHILD, SESSION_ID, "NEED_HELP"));
	}

	private MockHttpServletRequestBuilder prepExtensionRequest(String bearer, String body) {
		return post(PREP_EXTENSIONS_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder childLocationRequest(String body) {
		return post(CHILD_LOCATIONS_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, childBearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder helpRequest(String bearer) {
		return helpRequest(bearer, "NEED_HELP");
	}

	private MockHttpServletRequestBuilder helpRequest(String bearer, String type) {
		return post(HELP_REQUESTS_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"%s\"}".formatted(type));
	}

	private String childBearer() {
		return bearer(accessTokenFixture.user(CHILD_USER_ID, FAMILY_ID, Role.CHILD));
	}

	private String guardianBearer() {
		return bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN));
	}

	private MockHttpServletRequestBuilder telemetryRequest(String body) {
		return post(TELEMETRY_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, vehicleBearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder vehicleEventRequest(String body) {
		return post(VEHICLE_EVENTS_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, vehicleBearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private String vehicleBearer() {
		return bearer(accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID));
	}

	private static String vehicleEventBody(String type, String door) {
		return vehicleEventBody(EVENT_ID, type, door);
	}

	private static String vehicleEventBody(String eventId, String type, String door) {
		String doorField = door == null ? "" : ",\"door\":\"%s\"".formatted(door);
		return """
				{"eventId":"%s","type":"%s","occurredAt":"2026-11-09T18:20:30+09:00",\
				"location":{"lat":37.501234,"lng":127.039876}%s}
				""".formatted(eventId, type, doorField);
	}

	/** 정상 이벤트 본문(STOPPED)에서 필드 하나만 뺀다. */
	private static String vehicleEventBodyWithout(String missingField) {
		StringBuilder body = new StringBuilder("{");
		if (!"eventId".equals(missingField)) {
			body.append("\"eventId\":\"").append(EVENT_ID).append("\",");
		}
		if (!"type".equals(missingField)) {
			body.append("\"type\":\"STOPPED\",");
		}
		if (!"occurredAt".equals(missingField)) {
			body.append("\"occurredAt\":\"2026-11-09T18:20:30+09:00\",");
		}
		body.append("\"location\":{\"lat\":37.501234,\"lng\":127.039876}}");
		return body.toString();
	}

	private static ResultMatcher validationFailedOn(String field) {
		return result -> {
			status().isUnprocessableContent().match(result);
			commonErrorKeys().match(result);
			jsonPath("$.error.code").value("VALIDATION_FAILED").match(result);
			jsonPath("$.error.fields[*].field", hasItem(field)).match(result);
			jsonPath("$.error.fields[?(@.field == '" + field + "')].reason", everyItem(not(emptyOrNullString())))
					.match(result);
		};
	}

}

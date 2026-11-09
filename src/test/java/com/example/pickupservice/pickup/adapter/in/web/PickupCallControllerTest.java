package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallCommand;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeResult;
import com.example.pickupservice.pickup.application.port.in.CallHoldCommand;
import com.example.pickupservice.pickup.application.port.in.CallProgressView;
import com.example.pickupservice.pickup.application.port.in.CallResult;
import com.example.pickupservice.pickup.application.port.in.RevokeCallCommand;
import com.example.pickupservice.pickup.application.port.in.SendCallCommand;
import com.example.pickupservice.pickup.application.port.in.SessionCallView;
import com.example.pickupservice.pickup.application.port.in.SessionStatusView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.service.PickupErrors;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.domain.JoinCallMode;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.SESSION_ID;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.STATE_VERSION;
import static com.example.pickupservice.pickup.adapter.in.web.SessionViewFixture.sessionView;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class PickupCallControllerTest extends PickupWebTestSupport {

	private static final String SEND_CALL_PATH = "/v1/pickup-sessions/{sessionId}/calls";
	private static final String SEND_CALL_BODY = """
			{"stateVersion":15}
			""";
	private static final String REVOCATION_PATH = "/v1/calls/{callId}/revocation";
	private static final String ACKNOWLEDGEMENT_PATH = "/v1/calls/{callId}/acknowledgement";
	private static final String CALL_HOLD_PATH = "/v1/pickup-sessions/{sessionId}/call-hold";
	private static final String HOLD_CALL_BODY = """
			{"stateVersion":15}
			""";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final long TEACHER_USER_ID = 141L;
	private static final long CHILD_USER_ID = 31L;
	private static final long CALL_ID = 3301L;
	private static final String REVOKE_REASON = "아이가 아직 수업 중이에요";
	private static final String REVOKE_BODY = """
			{"stateVersion":15,"reason":"아이가 아직 수업 중이에요"}
			""";
	private static final AuthPrincipal VEHICLE = new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, FAMILY_ID, null);
	private static final AuthPrincipal TEACHER = user(TEACHER_USER_ID, Role.TEACHER);
	private static final AuthPrincipal CHILD = user(CHILD_USER_ID, Role.CHILD);

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	static Stream<AuthPrincipal> revokers() {
		return Stream.of(
				VEHICLE,
				user(GUARDIAN_USER_ID, Role.GUARDIAN),
				user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN),
				user(DELEGATE_USER_ID, Role.DELEGATE));
	}

	/**
	 * CALL-01 호출 보류 · 해제 권한: 차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE (CALL-03 철회와 같음).
	 */
	static Stream<AuthPrincipal> callHolders() {
		return revokers();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("revokers")
	@DisplayName("POST /v1/calls/{callId}/revocation은 200과 status.call REVOKED인 SessionView를 돌려준다")
	void revokeReturns200WithRevokedSessionView(AuthPrincipal principal) throws Exception {
		given(revokeCallUseCase.revoke(any())).willReturn(revokedSessionView());

		mockMvc.perform(revokeRequest(principal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.call").value("REVOKED"))
				.andExpect(jsonPath("$.call.callId").value(CALL_ID))
				.andExpect(jsonPath("$.call.progress.sentAt").value("2026-11-05T18:09:02+09:00"))
				.andExpect(jsonPath("$.children[*].phase", everyItem(is("WAITING"))));

		then(revokeCallUseCase).should().revoke(
				new RevokeCallCommand(principal, CALL_ID, STATE_VERSION, REVOKE_REASON));
	}

	@Test
	@DisplayName("POST /v1/calls/{callId}/revocation이 이미 철회된 호출이면 409 CALL_ALREADY_REVOKED를 돌려준다")
	void revokeAlreadyRevokedCallReturns409CallAlreadyRevoked() throws Exception {
		given(revokeCallUseCase.revoke(any())).willThrow(PickupErrors.callAlreadyRevoked());

		mockMvc.perform(revokeRequest(VEHICLE))
				.andExpect(commonError(HttpStatus.CONFLICT, "CALL_ALREADY_REVOKED"));

		then(revokeCallUseCase).should().revoke(
				new RevokeCallCommand(VEHICLE, CALL_ID, STATE_VERSION, REVOKE_REASON));
	}

	@Test
	@DisplayName("POST /v1/calls/{callId}/revocation을 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void revokeWithTeacherTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(revokeRequest(TEACHER))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(revokeCallUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/calls는 201과 callId · status를 돌려준다")
	void sendCallReturns201WithCallIdAndStatus() throws Exception {
		given(sendCallUseCase.send(any())).willReturn(new CallResult(CALL_ID, CallStatus.ACCEPTED));

		mockMvc.perform(sendCallRequest(VEHICLE))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"callId":3301,"status":"ACCEPTED"}
						""", JsonCompareMode.STRICT));

		then(sendCallUseCase).should().send(new SendCallCommand(VEHICLE, SESSION_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/calls가 가드 미충족이면 422 CALL_GUARD_UNMET과 unmet을 돌려준다")
	void sendCallWithUnmetGuardReturns422CallGuardUnmetWithUnmet() throws Exception {
		given(sendCallUseCase.send(any())).willThrow(PickupErrors.callGuardUnmet(List.of("G2", "G4")));

		mockMvc.perform(sendCallRequest(VEHICLE))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "CALL_GUARD_UNMET"))
				.andExpect(jsonPath("$.error.unmet", contains("G2", "G4")));

		then(sendCallUseCase).should().send(new SendCallCommand(VEHICLE, SESSION_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("POST /v1/pickup-sessions/{sessionId}/calls가 AUTO 가족이면 409 NOT_GUARDIAN_CONFIRM_MODE를 돌려준다")
	void sendCallInAutoFamilyReturns409NotGuardianConfirmMode() throws Exception {
		given(sendCallUseCase.send(any())).willThrow(PickupErrors.notGuardianConfirmMode());

		mockMvc.perform(sendCallRequest(VEHICLE))
				.andExpect(commonError(HttpStatus.CONFLICT, "NOT_GUARDIAN_CONFIRM_MODE"));

		then(sendCallUseCase).should().send(new SendCallCommand(VEHICLE, SESSION_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("POST /v1/calls/{callId}/acknowledgement를 차량 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void acknowledgeWithVehicleTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(post(ACKNOWLEDGEMENT_PATH, CALL_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(acknowledgeCallUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/calls/{callId}/acknowledgement는 200과 callId · status · acknowledgedAt을 돌려준다")
	void acknowledgeReturns200WithCallIdStatusAndAcknowledgedAt() throws Exception {
		given(acknowledgeCallUseCase.acknowledge(any())).willReturn(new AcknowledgeResult(
				CALL_ID, CallStatus.ACKNOWLEDGED, Instant.parse("2026-11-05T09:10:30Z")));

		mockMvc.perform(post(ACKNOWLEDGEMENT_PATH, CALL_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(CHILD))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"callId":3301,"status":"ACKNOWLEDGED","acknowledgedAt":"2026-11-05T18:10:30+09:00"}
						""", JsonCompareMode.STRICT));

		then(acknowledgeCallUseCase).should().acknowledge(new AcknowledgeCallCommand(CHILD, CALL_ID));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("callHolders")
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/call-hold는 200과 call.held true인 SessionView를 돌려준다")
	void holdCallReturns200WithCallHeldTrueSessionView(AuthPrincipal principal) throws Exception {
		given(holdCallUseCase.holdCall(any())).willReturn(callHoldSessionView(true));

		mockMvc.perform(holdCallRequest(principal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.status.call").value("NONE"))
				.andExpect(jsonPath("$.call.held").value(true))
				.andExpect(jsonPath("$.call.callId").value(nullValue()));

		then(holdCallUseCase).should().holdCall(new CallHoldCommand(principal, SESSION_ID, STATE_VERSION));
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/call-hold가 발송 뒤면 409 CALL_ALREADY_SENT를 돌려준다")
	void holdCallAfterSentReturns409CallAlreadySent() throws Exception {
		given(holdCallUseCase.holdCall(any())).willThrow(PickupErrors.callAlreadySent());

		mockMvc.perform(holdCallRequest(VEHICLE))
				.andExpect(commonError(HttpStatus.CONFLICT, "CALL_ALREADY_SENT"));

		then(holdCallUseCase).should().holdCall(new CallHoldCommand(VEHICLE, SESSION_ID, STATE_VERSION));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("callHolders")
	@DisplayName("DELETE /v1/pickup-sessions/{sessionId}/call-hold는 쿼리 stateVersion을 받아 200과 call.held false인 SessionView를 돌려준다")
	void releaseCallHoldWithQueryStateVersionReturns200WithCallHeldFalseSessionView(AuthPrincipal principal)
			throws Exception {
		given(holdCallUseCase.releaseCallHold(any())).willReturn(callHoldSessionView(false));

		mockMvc.perform(delete(CALL_HOLD_PATH, SESSION_ID)
						.queryParam("stateVersion", String.valueOf(STATE_VERSION))
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionId").value(SESSION_ID))
				.andExpect(jsonPath("$.stateVersion").value(STATE_VERSION + 1))
				.andExpect(jsonPath("$.call.held").value(false));

		then(holdCallUseCase).should().releaseCallHold(new CallHoldCommand(principal, SESSION_ID, STATE_VERSION));
		then(holdCallUseCase).should(never()).holdCall(any());
	}

	static Stream<Arguments> bodyRequestsWithoutStateVersion() {
		return Stream.of(
				Arguments.of(HttpMethod.POST, REVOCATION_PATH, CALL_ID, """
						{"reason":"아이가 아직 수업 중이에요"}
						"""),
				Arguments.of(HttpMethod.POST, SEND_CALL_PATH, SESSION_ID, "{}"),
				Arguments.of(HttpMethod.PUT, CALL_HOLD_PATH, SESSION_ID, "{}"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("bodyRequestsWithoutStateVersion")
	@DisplayName("세션을 바꾸는 요청 본문에 stateVersion이 없으면 422 VALIDATION_FAILED(fields stateVersion)를 돌려준다")
	void bodyWithoutStateVersionReturns422ValidationFailedOnStateVersion(
			HttpMethod method, String path, long pathId, String body) throws Exception {
		mockMvc.perform(request(method, path, pathId)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(VEHICLE))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("stateVersion"));

		then(revokeCallUseCase).shouldHaveNoInteractions();
		then(sendCallUseCase).shouldHaveNoInteractions();
		then(holdCallUseCase).shouldHaveNoInteractions();
	}

	/**
	 * CALL-01 호출 보류 · 해제 뒤 모습: call.held만 바뀌고(발송 전이라 callId 없음, status.call NONE) stateVersion 1 증가.
	 */
	private static SessionView callHoldSessionView(boolean held) {
		SessionView base = sessionView();
		SessionCallView call = base.call();
		return new SessionView(
				base.sessionId(),
				base.stateVersion() + 1,
				base.status(),
				base.spot(),
				base.eta(),
				new SessionCallView(call.mode(), call.callId(), held, call.scheduledAt(), call.guard(), call.progress()),
				base.children(),
				base.route(),
				base.boarding(),
				base.incident());
	}

	private MockHttpServletRequestBuilder holdCallRequest(AuthPrincipal principal) {
		return put(CALL_HOLD_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(HOLD_CALL_BODY);
	}

	/**
	 * 철회 뒤 모습: status.call REVOKED, call.callId 있음, 세션 자녀는 WAITING, stateVersion 1 증가.
	 */
	private static SessionView revokedSessionView() {
		SessionView base = sessionView();
		return new SessionView(
				base.sessionId(),
				base.stateVersion() + 1,
				new SessionStatusView(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, "REVOKED", SpotStatus.CONFIRMED),
				base.spot(),
				base.eta(),
				new SessionCallView(
						JoinCallMode.AUTO, CALL_ID, false, base.call().scheduledAt(), base.call().guard(),
						new CallProgressView(
								Instant.parse("2026-11-05T09:09:00Z"), Instant.parse("2026-11-05T09:09:02Z"),
								null, null)),
				base.children(),
				base.route(),
				null,
				null);
	}

	private MockHttpServletRequestBuilder sendCallRequest(AuthPrincipal principal) {
		return post(SEND_CALL_PATH, SESSION_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(SEND_CALL_BODY);
	}

	private MockHttpServletRequestBuilder revokeRequest(AuthPrincipal principal) {
		return post(REVOCATION_PATH, CALL_ID)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(REVOKE_BODY);
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

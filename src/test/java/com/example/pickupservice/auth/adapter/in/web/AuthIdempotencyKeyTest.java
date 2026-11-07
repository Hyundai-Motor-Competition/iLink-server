package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.InviteInfo;
import com.example.pickupservice.auth.application.port.in.LinkCodeInfo;
import com.example.pickupservice.auth.application.port.in.LinkCodeResult;
import com.example.pickupservice.auth.application.port.in.LinkStatus;
import com.example.pickupservice.auth.application.port.in.LinkStatusResult;
import com.example.pickupservice.auth.application.port.in.TokenResult;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class AuthIdempotencyKeyTest extends AuthWebTestSupport {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CHILD_USER_ID = 131L;
	private static final long CHILD_DEVICE_ID = 401L;
	private static final long VEHICLE_ID = 8_801L;
	private static final long VEHICLE_DEVICE_ID = 108_801L;
	private static final long CODE_ID = 5012L;
	private static final String CODE = "K7P2QX9M";
	private static final String INVITE_TOKEN = "f3a9c1e7b5d24f6a8c0e2b4d6f8a1c3e";
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-05T12:10:00Z");

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("Idempotency-Key는 POST /v1/vehicle-devices · POST /v1/vehicle-links · POST /v1/families/me/members/{userId}/invites · POST /v1/invites/{token}/acceptance에만 요구된다")
	void idempotencyKeyIsRequiredOnlyForRegisterDeviceApproveLinkIssueInviteAndAcceptInvite() {
		given(refreshTokenUseCase.refresh(anyString()))
				.willReturn(new TokenResult("eyJ.new-access-token", "rt_new-refresh-token", EXPIRES_AT));
		given(issueLinkCodeUseCase.issueLinkCode(any()))
				.willReturn(new LinkCodeResult(CODE_ID, CODE, "pickup://link?code=" + CODE, EXPIRES_AT, 4));
		given(getLinkStatusUseCase.getLinkStatus(anyLong(), anyLong(), anyLong()))
				.willReturn(new LinkStatusResult(LinkStatus.WAITING, null, null, null, null, 2));
		given(checkLinkCodeUseCase.checkLinkCode(anyString(), anyString()))
				.willReturn(new LinkCodeInfo(LinkMode.NEW, "아이오닉 5", "12가 ****", EXPIRES_AT));
		given(getInviteUseCase.getInvite(anyString())).willReturn(new InviteInfo("김보호", "민준", Role.CHILD,
				List.of(ConsentType.TERMS, ConsentType.PRIVACY, ConsentType.LOCATION, ConsentType.PUSH), EXPIRES_AT));

		assertAll(
				() -> mockMvc.perform(post("/v1/vehicle-devices")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"vin":"KMHL14JA5PA123456","model":"아이오닉 5"}
										"""))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(post("/v1/vehicle-links")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"code":"K7P2QX9M","mode":"NEW","consents":[{"type":"TERMS","agreed":true},{"type":"PRIVACY","agreed":true}],"platform":"ANDROID","pushToken":"fcm-token","name":"김보호"}
										"""))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(post("/v1/families/me/members/{userId}/invites", CHILD_USER_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"replaceDevice":false}
										""")))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(post("/v1/invites/{token}/acceptance", INVITE_TOKEN)
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"consents":[{"type":"TERMS","agreed":true},{"type":"PRIVACY","agreed":true},{"type":"LOCATION","agreed":true},{"type":"PUSH","agreed":true}],"platform":"ANDROID","pushToken":"fcm-token"}
										"""))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(post("/v1/auth/refresh")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"refreshToken":"rt_old-refresh-token"}
										"""))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(put("/v1/devices/me/push-token")
								.header(HttpHeaders.AUTHORIZATION, bearer(childTokenWithDeviceId()))
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"platform":"ANDROID","pushToken":"fcm-token"}
										"""))
						.andExpect(status().isNoContent()),
				() -> mockMvc.perform(withVehicle(delete("/v1/vehicle-devices/me/session")))
						.andExpect(status().isNoContent()),
				() -> mockMvc.perform(withVehicle(post("/v1/vehicle-link-codes")))
						.andExpect(status().isCreated()),
				() -> mockMvc.perform(withVehicle(get("/v1/vehicle-link-codes/{codeId}/status", CODE_ID)))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(get("/v1/vehicle-link-codes/by-code/{code}", CODE))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(get("/v1/invites/{token}", INVITE_TOKEN))
						.andExpect(status().isOk()));

		then(registerVehicleDeviceUseCase).shouldHaveNoInteractions();
		then(approveLinkUseCase).shouldHaveNoInteractions();
		then(issueInviteUseCase).shouldHaveNoInteractions();
		then(acceptInviteUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder withGuardian(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION,
				bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN)));
	}

	private MockHttpServletRequestBuilder withVehicle(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(VEHICLE_ID),
				"principalType", PrincipalType.VEHICLE.name(),
				"familyId", FAMILY_ID,
				"deviceId", VEHICLE_DEVICE_ID))));
	}

	private String childTokenWithDeviceId() {
		return accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(CHILD_USER_ID),
				"principalType", PrincipalType.USER.name(),
				"familyId", FAMILY_ID,
				"role", Role.CHILD.name(),
				"deviceId", CHILD_DEVICE_ID));
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

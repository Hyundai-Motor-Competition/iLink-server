package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.TokenResult;
import com.example.pickupservice.auth.application.port.in.UpdatePushTokenCommand;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.Map;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class AuthControllerTest extends AuthWebTestSupport {

	private static final String REFRESH_PATH = "/v1/auth/refresh";
	private static final String OLD_REFRESH_TOKEN = "rt_old-refresh-token";
	private static final String NEW_ACCESS_TOKEN = "eyJ.new-access-token";
	private static final String NEW_REFRESH_TOKEN = "rt_new-refresh-token";
	private static final Instant ACCESS_TOKEN_EXPIRES_AT = Instant.parse("2026-11-07T12:30:00Z");
	private static final String ACCESS_TOKEN_EXPIRES_AT_KST = "2026-11-07T21:30:00+09:00";
	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;
	private static final String PUSH_TOKEN_PATH = "/v1/devices/me/push-token";
	private static final String PUSH_TOKEN = "fcm-push-token-123";
	private static final long DEVICE_ID = 401L;
	private static final long VEHICLE_ID = 77L;
	private static final long VEHICLE_DEVICE_ID = 301L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("POST /v1/auth/refresh는 인증 헤더 없이 200과 accessToken · refreshToken · accessTokenExpiresAt을 돌려준다")
	void refreshWithoutAuthorizationHeaderReturns200WithNewTokens() throws Exception {
		given(refreshTokenUseCase.refresh(OLD_REFRESH_TOKEN))
				.willReturn(new TokenResult(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, ACCESS_TOKEN_EXPIRES_AT));

		mockMvc.perform(refreshRequest(OLD_REFRESH_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value(NEW_ACCESS_TOKEN))
				.andExpect(jsonPath("$.refreshToken").value(NEW_REFRESH_TOKEN))
				.andExpect(jsonPath("$.accessTokenExpiresAt").value(ACCESS_TOKEN_EXPIRES_AT_KST));
	}

	@Test
	@DisplayName("POST /v1/auth/refresh는 본문의 refreshToken 원문을 RefreshTokenUseCase.refresh에 그대로 넘긴다")
	void refreshPassesRequestRefreshTokenToUseCase() throws Exception {
		given(refreshTokenUseCase.refresh(anyString()))
				.willReturn(new TokenResult(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, ACCESS_TOKEN_EXPIRES_AT));

		mockMvc.perform(refreshRequest(OLD_REFRESH_TOKEN))
				.andExpect(status().isOk());

		then(refreshTokenUseCase).should().refresh(OLD_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("POST /v1/auth/refresh에 폐기된 토큰을 보내면 401 REFRESH_TOKEN_INVALID를 돌려준다")
	void refreshWithRevokedTokenReturns401RefreshTokenInvalid() throws Exception {
		given(refreshTokenUseCase.refresh(OLD_REFRESH_TOKEN))
				.willThrow(BusinessException.of(
						HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "refresh 토큰이 유효하지 않습니다"));

		mockMvc.perform(refreshRequest(OLD_REFRESH_TOKEN))
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID"));
	}

	@Test
	@DisplayName("POST /v1/auth/refresh에 만료된 access 토큰이 Authorization 헤더로 붙어 와도 401이 아니라 200과 새 토큰을 돌려준다")
	void refreshWithExpiredAccessTokenHeaderStillReturns200() throws Exception {
		given(refreshTokenUseCase.refresh(OLD_REFRESH_TOKEN))
				.willReturn(new TokenResult(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, ACCESS_TOKEN_EXPIRES_AT));
		String expiredAccessToken = accessTokenFixture.expiredUser(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(refreshRequest(OLD_REFRESH_TOKEN)
						.header(HttpHeaders.AUTHORIZATION, bearer(expiredAccessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value(NEW_ACCESS_TOKEN))
				.andExpect(jsonPath("$.refreshToken").value(NEW_REFRESH_TOKEN));
	}

	@Test
	@DisplayName("PUT /v1/devices/me/push-token은 204를 돌려준다")
	void updatePushTokenReturns204() throws Exception {
		mockMvc.perform(pushTokenRequest(userTokenWithDeviceId(DEVICE_ID), "ANDROID", PUSH_TOKEN))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	@DisplayName("PUT /v1/devices/me/push-token은 access 토큰의 deviceId 클레임과 본문의 platform · pushToken을 UpdatePushTokenCommand에 담아 넘긴다")
	void updatePushTokenPassesDeviceIdClaimAndBodyToUseCase() throws Exception {
		mockMvc.perform(pushTokenRequest(userTokenWithDeviceId(DEVICE_ID), "IOS", PUSH_TOKEN))
				.andExpect(status().isNoContent());

		then(updatePushTokenUseCase).should()
				.updatePushToken(new UpdatePushTokenCommand(DEVICE_ID, Platform.IOS, PUSH_TOKEN));
	}

	@Test
	@DisplayName("PUT /v1/devices/me/push-token은 VEHICLE 토큰으로 불러도 204를 돌려주고 platform VEHICLE을 넘긴다")
	void updatePushTokenWithVehicleTokenReturns204() throws Exception {
		String vehicleToken = accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(VEHICLE_ID),
				"principalType", PrincipalType.VEHICLE.name(),
				"familyId", FAMILY_ID,
				"deviceId", VEHICLE_DEVICE_ID));

		mockMvc.perform(pushTokenRequest(vehicleToken, "VEHICLE", PUSH_TOKEN))
				.andExpect(status().isNoContent());

		then(updatePushTokenUseCase).should()
				.updatePushToken(new UpdatePushTokenCommand(VEHICLE_DEVICE_ID, Platform.VEHICLE, PUSH_TOKEN));
	}

	@Test
	@DisplayName("PUT /v1/devices/me/push-token에 256자 pushToken을 보내면 422 VALIDATION_FAILED를 돌려준다")
	void updatePushTokenWith256CharPushTokenReturns422() throws Exception {
		mockMvc.perform(pushTokenRequest(userTokenWithDeviceId(DEVICE_ID), "ANDROID", "a".repeat(256)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("pushToken"));

		then(updatePushTokenUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/devices/me/push-token에 255자 pushToken을 보내면 204를 돌려준다")
	void updatePushTokenWith255CharPushTokenReturns204() throws Exception {
		mockMvc.perform(pushTokenRequest(userTokenWithDeviceId(DEVICE_ID), "ANDROID", "a".repeat(255)))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("PUT /v1/devices/me/push-token을 인증 헤더 없이 부르면 401 UNAUTHORIZED를 돌려준다")
	void updatePushTokenWithoutAuthorizationHeaderReturns401() throws Exception {
		mockMvc.perform(put(PUSH_TOKEN_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(pushTokenBody("ANDROID", PUSH_TOKEN)))
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));

		then(updatePushTokenUseCase).shouldHaveNoInteractions();
	}

	private String userTokenWithDeviceId(long deviceId) {
		return accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(USER_ID),
				"principalType", PrincipalType.USER.name(),
				"familyId", FAMILY_ID,
				"role", Role.CHILD.name(),
				"deviceId", deviceId));
	}

	private static MockHttpServletRequestBuilder pushTokenRequest(String accessToken, String platform, String pushToken) {
		return put(PUSH_TOKEN_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(pushTokenBody(platform, pushToken));
	}

	private static String pushTokenBody(String platform, String pushToken) {
		return "{\"platform\": \"" + platform + "\", \"pushToken\": \"" + pushToken + "\"}";
	}

	private static MockHttpServletRequestBuilder refreshRequest(String refreshToken) {
		return post(REFRESH_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\": \"" + refreshToken + "\"}");
	}

}

package com.example.pickupservice.common.security;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class SecurityConfigTest extends IntegrationTestSupport {

	private static final String SECURED_PATH = "/test/secured/ping";
	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("인증이 필요한 GET API를 Authorization 헤더 없이 부르면 401과 JSON 본문의 error.code UNAUTHORIZED · 비어 있지 않은 error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void getWithoutAuthorizationHeaderReturns401WithUnauthorizedError() throws Exception {
		mockMvc.perform(get(SECURED_PATH))
				.andExpect(unauthorizedError());
	}

	@Test
	@DisplayName("인증이 필요한 POST API를 Authorization 헤더 없이 JSON 본문과 함께 부르면 403이 아니라 401과 JSON 본문의 error.code UNAUTHORIZED · 비어 있지 않은 error.message · error.retryAfterSeconds(null) 키를 돌려준다")
	void postWithoutAuthorizationHeaderReturns401WithUnauthorizedError() throws Exception {
		mockMvc.perform(post(SECURED_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"minwoo\"}"))
				.andExpect(unauthorizedError());
	}

	@Test
	@DisplayName("서명은 올바르지만 만료 시각이 10분 지난 USER access 토큰으로 인증이 필요한 GET API를 부르면 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void getWithExpiredAccessTokenReturns401WithUnauthorizedError() throws Exception {
		String expiredToken = accessTokenFixture.expiredUser(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(get(SECURED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
				.andExpect(unauthorizedError());
	}

	@Test
	@DisplayName("같은 주체 · 같은 서명 키로 만든 만료 전 USER access 토큰으로 같은 GET API를 부르면 인증을 통과해 200과 컨트롤러 응답 pong을 돌려준다")
	void getWithUnexpiredAccessTokenReachesController() throws Exception {
		String validToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(get(SECURED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(validToken)))
				.andExpect(status().isOk())
				.andExpect(content().string("pong"));
	}

	@Test
	@DisplayName("같은 클레임의 만료 전 USER access 토큰이라도 서버 키가 아닌 다른 임의 키로 HS256 서명했으면 인증이 필요한 GET API에서 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void getWithTokenSignedByOtherKeyReturns401WithUnauthorizedError() throws Exception {
		String otherKeyToken = accessTokenFixture.userSignedWithOtherKey(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(get(SECURED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(otherKeyToken)))
				.andExpect(unauthorizedError());
	}

	@Test
	@DisplayName("서버 키로 서명한 만료 전 USER access 토큰의 서명 부분 첫 글자를 바꾸면 인증이 필요한 GET API에서 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void getWithTamperedSignatureReturns401WithUnauthorizedError() throws Exception {
		String validToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);
		String tamperedToken = withTamperedSignature(validToken);

		assertThat(tamperedToken).isNotEqualTo(validToken);
		mockMvc.perform(get(SECURED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tamperedToken)))
				.andExpect(unauthorizedError());
	}

	@Test
	@DisplayName("서버 키로 서명한 CHILD 토큰의 본문 role을 GUARDIAN으로 바꾸고 원래 서명을 그대로 두면 서명이 본문과 맞지 않아 인증이 필요한 GET API에서 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void getWithTamperedPayloadAndOriginalSignatureReturns401WithUnauthorizedError() throws Exception {
		String childToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.CHILD);
		String escalatedToken = withPayload(childToken, payloadJson(childToken).replace("\"CHILD\"", "\"GUARDIAN\""));

		assertThat(payloadJson(escalatedToken)).contains("\"GUARDIAN\"").doesNotContain("\"CHILD\"");
		mockMvc.perform(get(SECURED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(escalatedToken)))
				.andExpect(unauthorizedError());
	}

	@Test
	@DisplayName("서버 키로 서명한 만료 전 USER access 토큰의 헤더 alg를 none으로 바꾸고 서명을 비운 토큰으로 인증이 필요한 GET API를 부르면 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void getWithAlgNoneUnsignedTokenReturns401WithUnauthorizedError() throws Exception {
		String validToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);
		String unsignedToken = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + tokenPart(validToken, 1) + ".";

		mockMvc.perform(get(SECURED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(unsignedToken)))
				.andExpect(unauthorizedError());
	}

	@Test
	@DisplayName("서버 키로 HS256 서명했고 sub 101 · principalType USER · familyId 12 · role GUARDIAN · iat를 담았지만 exp 클레임이 없는 access 토큰으로 인증이 필요한 GET API를 부르면 200이 아니라 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void getWithTokenWithoutExpiryClaimReturns401WithUnauthorizedError() throws Exception {
		String tokenWithoutExpiry = accessTokenFixture.signedWithClaimsWithoutExpiry(guardianClaims());

		assertThat(payloadJson(tokenWithoutExpiry)).contains("\"iat\"").doesNotContain("\"exp\"");
		mockMvc.perform(get(SECURED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tokenWithoutExpiry)))
				.andExpect(unauthorizedError());
	}

	private static Map<String, Object> guardianClaims() {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", String.valueOf(USER_ID));
		claims.put("principalType", "USER");
		claims.put("familyId", FAMILY_ID);
		claims.put("role", "GUARDIAN");
		return claims;
	}

	private static String withTamperedSignature(String token) {
		int signatureStart = token.lastIndexOf('.') + 1;
		char replaced = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
		return token.substring(0, signatureStart) + replaced + token.substring(signatureStart + 1);
	}

	private static String withPayload(String token, String payloadJson) {
		return tokenPart(token, 0) + "." + base64Url(payloadJson) + "." + tokenPart(token, 2);
	}

	private static String payloadJson(String token) {
		return new String(Base64.getUrlDecoder().decode(tokenPart(token, 1)), StandardCharsets.UTF_8);
	}

	private static String tokenPart(String token, int index) {
		return token.split("\\.", -1)[index];
	}

	private static String base64Url(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private static ResultMatcher unauthorizedError() {
		return commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
	}

	@RestController
	static class SecuredTestController {

		@GetMapping(SECURED_PATH)
		public String getPing() {
			return "pong";
		}

		@PostMapping(SECURED_PATH)
		public String postPing() {
			return "pong";
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(SecuredTestController.class)
	static class SecuredTestEndpointConfiguration {
	}

}

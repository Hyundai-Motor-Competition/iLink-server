package com.example.pickupservice.common.security;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RequiredArgsConstructor
class AuthPrincipalJwtConverterTest extends IntegrationTestSupport {

	private static final String AUTHENTICATED_PATH = "/test/principal-claims/ping";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	static Stream<Arguments> malformedPrincipalClaims() {
		return Stream.of(
				argumentSet("principalType 클레임 없음", guardianClaimsWithout("principalType")),
				argumentSet("principalType이 없는 값 ADMIN", guardianClaimsWith("principalType", "ADMIN")),
				argumentSet("sub가 숫자가 아닌 abc", guardianClaimsWith("sub", "abc")),
				argumentSet("USER인데 role이 없는 값 SUPERUSER", guardianClaimsWith("role", "SUPERUSER")),
				argumentSet("familyId가 숫자가 아닌 x", guardianClaimsWith("familyId", "x")));
	}

	@ParameterizedTest(name = "{argumentSetName}")
	@MethodSource("malformedPrincipalClaims")
	@DisplayName("서버 키로 서명한 만료 전 토큰이라도 주체 클레임이 빠졌거나 틀리면(principalType 없음 · 없는 principalType ADMIN · 숫자가 아닌 sub abc · USER의 없는 role SUPERUSER · 숫자가 아닌 familyId x) 인증이 필요한 GET API에서 500이 아니라 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void tokenWithMalformedPrincipalClaimsReturns401WithUnauthorizedError(Map<String, Object> claims) throws Exception {
		String token = accessTokenFixture.signedWithClaims(claims);

		mockMvc.perform(get(AUTHENTICATED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));
	}

	private static Map<String, Object> guardianClaimsWith(String name, Object value) {
		Map<String, Object> claims = guardianClaims();
		claims.put(name, value);
		return claims;
	}

	private static Map<String, Object> guardianClaimsWithout(String name) {
		Map<String, Object> claims = guardianClaims();
		claims.remove(name);
		return claims;
	}

	private static Map<String, Object> guardianClaims() {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", "101");
		claims.put("principalType", "USER");
		claims.put("familyId", 12L);
		claims.put("role", "GUARDIAN");
		return claims;
	}

}

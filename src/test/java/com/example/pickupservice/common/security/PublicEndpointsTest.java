package com.example.pickupservice.common.security;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class PublicEndpointsTest extends IntegrationTestSupport {

	private static final String PUBLIC_PATTERN = "/test/public/**";
	private static final String PUBLIC_ECHO_PATH = "/test/public/echo";
	private static final String NOT_LISTED_ECHO_PATH = "/test/not-public/echo";
	private static final String ECHO_BODY = "{\"message\": \"hello\"}";
	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("PublicEndpoints 빈에 POST /test/public/** 를 등록하면 POST /test/public/echo를 Authorization 헤더 없이 불러도 401이 아니라 200과 컨트롤러 응답을 돌려준다")
	void listedPublicEndpointWithoutTokenReachesController() throws Exception {
		mockMvc.perform(postEcho(PUBLIC_ECHO_PATH))
				.andExpect(echoedWithoutPrincipal());
	}

	@Test
	@DisplayName("PublicEndpoints 빈에 등록한 POST /test/public/echo에 만료 시각이 10분 지난 USER Bearer 토큰을 붙여 불러도 토큰을 읽지 않아 401이 아니라 200과 컨트롤러 응답을 돌려준다")
	void listedPublicEndpointWithExpiredBearerTokenReachesController() throws Exception {
		String expiredToken = accessTokenFixture.expiredUser(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(postEcho(PUBLIC_ECHO_PATH).header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
				.andExpect(echoedWithoutPrincipal());
	}

	@Test
	@DisplayName("PublicEndpoints 빈에 등록한 POST /test/public/echo에 서버 키로 서명한 만료 전 USER Bearer 토큰을 붙여도 토큰을 읽지 않아 컨트롤러는 AuthPrincipal 없이 실행된다")
	void listedPublicEndpointIgnoresValidBearerToken() throws Exception {
		String validToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(postEcho(PUBLIC_ECHO_PATH).header(HttpHeaders.AUTHORIZATION, bearer(validToken)))
				.andExpect(echoedWithoutPrincipal());
	}

	@Test
	@DisplayName("PublicEndpoints 빈에 POST만 등록한 경로를 GET /test/public/echo로 Authorization 헤더 없이 부르면 공개가 아니므로 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void unlistedMethodOnPublicPathWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get(PUBLIC_ECHO_PATH))
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));
	}

	@Test
	@DisplayName("PublicEndpoints 빈에 등록하지 않은 경로 POST /test/not-public/echo를 Authorization 헤더 없이 부르면 401과 error.code UNAUTHORIZED 공통 오류 본문을 돌려준다")
	void unlistedPathWithoutTokenReturns401() throws Exception {
		mockMvc.perform(postEcho(NOT_LISTED_ECHO_PATH))
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));
	}

	private static MockHttpServletRequestBuilder postEcho(String path) {
		return post(path).contentType(MediaType.APPLICATION_JSON).content(ECHO_BODY);
	}

	private static ResultMatcher echoedWithoutPrincipal() {
		return result -> {
			status().isOk().match(result);
			jsonPath("$.message").value("hello").match(result);
			jsonPath("$.principalPresent").value(false).match(result);
		};
	}

	record EchoRequest(String message) {
	}

	record EchoResponse(String message, boolean principalPresent) {
	}

	@RestController
	static class PublicEchoTestController {

		@PostMapping({PUBLIC_ECHO_PATH, NOT_LISTED_ECHO_PATH})
		public EchoResponse postEcho(@RequestBody EchoRequest request, @AuthenticationPrincipal AuthPrincipal principal) {
			return new EchoResponse(request.message(), principal != null);
		}

		@GetMapping(PUBLIC_ECHO_PATH)
		public EchoResponse getEcho(@AuthenticationPrincipal AuthPrincipal principal) {
			return new EchoResponse("hello", principal != null);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(PublicEchoTestController.class)
	static class PublicEchoTestEndpointConfiguration {

		@Bean
		PublicEndpoints publicEchoTestEndpoints() {
			return new PublicEndpoints(List.of(new PublicEndpoint(HttpMethod.POST, PUBLIC_PATTERN)));
		}

	}

}

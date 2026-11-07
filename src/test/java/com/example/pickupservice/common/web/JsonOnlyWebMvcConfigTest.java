package com.example.pickupservice.common.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class JsonOnlyWebMvcConfigTest extends IntegrationTestSupport {

	private static final String SAMPLE_PATH = "/test/json-only/sample";
	private static final String BUSINESS_ERROR_PATH = "/test/json-only/business-error";
	private static final String UNMAPPED_PATH = "/test/json-only/no-such-endpoint";

	private static final String SAMPLE_NAME = "minwoo";
	private static final int SAMPLE_GRADE = 3;
	private static final String DOMAIN_CODE = "SOME_DOMAIN_CODE";
	private static final String DOMAIN_CODE_MESSAGE = "도메인 규칙에 맞지 않는 요청입니다";

	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("produces 없이 record를 돌려주는 GET API를 Accept application/xml만 담아 부르면 XML로 나가지 않고 406과 Content-Type application/json 본문의 error.code NOT_ACCEPTABLE · error.message · error.retryAfterSeconds(null) 키를 돌려준다")
	void xmlOnlyAcceptOnRecordEndpointReturns406JsonErrorInsteadOfXml() throws Exception {
		mockMvc.perform(authorized(get(SAMPLE_PATH))
						.accept(MediaType.APPLICATION_XML))
				.andExpect(notXml())
				.andExpect(commonError(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE"));
	}

	@ParameterizedTest(name = "Accept {0}")
	@ValueSource(strings = {
			"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
			"application/xml, application/json"})
	@DisplayName("produces 없이 record를 돌려주는 GET API를 application/xml을 JSON · */*보다 앞세운 Accept로 부르면 XML이 아니라 200과 Content-Type application/json 본문의 name · grade를 돌려준다")
	void acceptPreferringXmlOverJsonOrWildcardReturns200Json(String accept) throws Exception {
		mockMvc.perform(authorized(get(SAMPLE_PATH))
						.header(HttpHeaders.ACCEPT, accept))
				.andExpect(notXml())
				.andExpect(sampleJson());
	}

	@Test
	@DisplayName("produces 없이 record를 돌려주는 GET API를 Accept 헤더 없이 부르면 200과 Content-Type application/json 본문의 name · grade를 돌려준다")
	void noAcceptHeaderOnRecordEndpointReturns200Json() throws Exception {
		mockMvc.perform(authorized(get(SAMPLE_PATH)))
				.andExpect(sampleJson());
	}

	@Test
	@DisplayName("produces 없이 record를 돌려주는 GET API를 Accept application/json으로 부르면 200과 Content-Type application/json 본문의 name · grade를 돌려준다")
	void jsonAcceptOnRecordEndpointReturns200Json() throws Exception {
		mockMvc.perform(authorized(get(SAMPLE_PATH))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(sampleJson());
	}

	@Test
	@DisplayName("매핑되지 않은 경로를 인증 토큰과 Accept application/xml로 부르면 XML이 아니라 404와 Content-Type application/json 본문의 error.code NOT_FOUND 공통 오류를 돌려준다")
	void unmappedPathWithXmlAcceptReturns404JsonError() throws Exception {
		mockMvc.perform(authorized(get(UNMAPPED_PATH))
						.accept(MediaType.APPLICATION_XML))
				.andExpect(notXml())
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"));
	}

	@Test
	@DisplayName("produces 없는 GET API가 BusinessException(409, SOME_DOMAIN_CODE, 메시지)을 던질 때 Accept application/xml로 불렀어도 XML이 아니라 409와 Content-Type application/json 본문의 error.code SOME_DOMAIN_CODE 공통 오류를 돌려준다")
	void businessExceptionWithXmlAcceptReturnsJsonError() throws Exception {
		mockMvc.perform(authorized(get(BUSINESS_ERROR_PATH))
						.accept(MediaType.APPLICATION_XML))
				.andExpect(notXml())
				.andExpect(commonError(HttpStatus.CONFLICT, DOMAIN_CODE))
				.andExpect(jsonPath("$.error.message").value(DOMAIN_CODE_MESSAGE));
	}

	@Test
	@DisplayName("인증이 필요한 GET API를 Authorization 헤더 없이 Accept application/xml로 부르면 XML이 아니라 401과 Content-Type application/json 본문의 error.code UNAUTHORIZED 공통 오류를 돌려준다")
	void missingTokenWithXmlAcceptReturns401JsonError() throws Exception {
		mockMvc.perform(get(SAMPLE_PATH)
						.accept(MediaType.APPLICATION_XML))
				.andExpect(notXml())
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));
	}

	private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN)));
	}

	private static ResultMatcher notXml() {
		return result -> {
			content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
			content().string(not(startsWith("<"))).match(result);
		};
	}

	private static ResultMatcher sampleJson() {
		return result -> {
			status().isOk().match(result);
			content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
			jsonPath("$.name").value(SAMPLE_NAME).match(result);
			jsonPath("$.grade").value(SAMPLE_GRADE).match(result);
		};
	}

	record SampleResponse(String name, int grade) {
	}

	@RestController
	static class JsonOnlyTestController {

		@GetMapping(SAMPLE_PATH)
		public SampleResponse sample() {
			return new SampleResponse(SAMPLE_NAME, SAMPLE_GRADE);
		}

		@GetMapping(BUSINESS_ERROR_PATH)
		public SampleResponse throwBusiness() {
			throw BusinessException.of(HttpStatus.CONFLICT, DOMAIN_CODE, DOMAIN_CODE_MESSAGE);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(JsonOnlyTestController.class)
	static class JsonOnlyTestEndpointConfiguration {
	}

}

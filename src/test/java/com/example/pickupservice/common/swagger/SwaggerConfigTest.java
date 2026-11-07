package com.example.pickupservice.common.swagger;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class SwaggerConfigTest extends IntegrationTestSupport {

	private static final String API_DOCS_PATH = "/v3/api-docs";
	private static final String SWAGGER_CONFIG_PATH = "/v3/api-docs/swagger-config";
	private static final String SWAGGER_UI_SHORTCUT_PATH = "/swagger-ui.html";
	private static final String SWAGGER_UI_INDEX_PATH = "/swagger-ui/index.html";
	private static final String BEARER_SCHEME_NAME = "bearerAuth";

	private final MockMvc mockMvc;

	@Test
	@DisplayName("dev 프로필에서 토큰 없이 GET /v3/api-docs를 부르면 401이 아니라 200과 openapi 3 버전 필드 · info를 담은 JSON 문서를 돌려준다")
	void apiDocsAreServedWithoutTokenOnDevProfile() throws Exception {
		mockMvc.perform(get(API_DOCS_PATH))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.openapi").value(startsWith("3.")))
				.andExpect(jsonPath("$.info").exists());
	}

	@Test
	@DisplayName("dev 프로필의 GET /v3/api-docs 문서는 components.securitySchemes에 type http · scheme bearer · bearerFormat JWT인 bearerAuth 스키마를 담고 전역 security 요구에 bearerAuth를 건다")
	void apiDocsContainBearerJwtSecuritySchemeAppliedGlobally() throws Exception {
		mockMvc.perform(get(API_DOCS_PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes." + BEARER_SCHEME_NAME + ".type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes." + BEARER_SCHEME_NAME + ".scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes." + BEARER_SCHEME_NAME + ".bearerFormat").value("JWT"))
				.andExpect(jsonPath("$.security[*]." + BEARER_SCHEME_NAME).isNotEmpty());
	}

	@Test
	@DisplayName("dev 프로필에서 토큰 없이 GET /v3/api-docs/swagger-config를 부르면 401이 아니라 200과 문서 주소 url /v3/api-docs를 담은 JSON을 돌려준다")
	void swaggerUiConfigIsServedWithoutTokenOnDevProfile() throws Exception {
		mockMvc.perform(get(SWAGGER_CONFIG_PATH))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.url").value(API_DOCS_PATH));
	}

	@Test
	@DisplayName("dev 프로필에서 토큰 없이 GET /swagger-ui.html을 부르면 401이 아니라 /swagger-ui/index.html로 리다이렉트한다")
	void swaggerUiShortcutRedirectsToIndexWithoutTokenOnDevProfile() throws Exception {
		mockMvc.perform(get(SWAGGER_UI_SHORTCUT_PATH))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string(HttpHeaders.LOCATION, containsString(SWAGGER_UI_INDEX_PATH)));
	}

	@Test
	@DisplayName("dev 프로필에서 토큰 없이 GET /swagger-ui/index.html을 부르면 401이 아니라 200과 스웨거 UI HTML 화면을 돌려준다")
	void swaggerUiIndexIsServedWithoutTokenOnDevProfile() throws Exception {
		mockMvc.perform(get(SWAGGER_UI_INDEX_PATH))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(containsString("swagger-ui")));
	}

}

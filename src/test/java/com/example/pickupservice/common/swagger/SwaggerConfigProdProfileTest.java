package com.example.pickupservice.common.swagger;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.TestPostgisConfiguration;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import({TestPostgisConfiguration.class, AccessTokenFixture.class})
@RequiredArgsConstructor
class SwaggerConfigProdProfileTest {

	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html"})
	@DisplayName("prod 프로필에서 토큰 없이 API 문서 · 스웨거 UI 경로를 부르면 문서도 401도 아닌 404 NOT_FOUND 공통 오류를 돌려준다")
	void docsAreNotFoundWithoutTokenOnProdProfile(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html"})
	@DisplayName("prod 프로필에서 만료 전 GUARDIAN USER 토큰을 붙여 API 문서 · 스웨거 UI 경로를 불러도 문서 없이 404 NOT_FOUND 공통 오류를 돌려준다")
	void docsAreNotFoundWithUserTokenOnProdProfile(String path) throws Exception {
		String userToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"));
	}

}

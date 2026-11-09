package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.DataSettingsCommand;
import com.example.pickupservice.pickup.application.port.in.DataSettingsResult;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OUT-04. 권한은 GUARDIAN만이고 Idempotency-Key는 요구하지 않으므로(#203) 요청에 그 헤더를 싣지 않는다.
 */
@RequiredArgsConstructor
class PickupDataSettingsControllerTest extends PickupWebTestSupport {

	private static final String DATA_SETTINGS_PATH = "/v1/guardians/me/data-settings";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final int RETENTION_DAYS = 90;
	private static final AuthPrincipal GUARDIAN = user(GUARDIAN_USER_ID, Role.GUARDIAN);
	private static final AuthPrincipal CO_GUARDIAN = user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN);

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@ParameterizedTest(name = "learningConsent {0}")
	@ValueSource(booleans = {true, false})
	@DisplayName("PUT /v1/guardians/me/data-settings는 200과 learningConsent · retentionDays를 돌려준다")
	void updateDataSettingsReturns200WithLearningConsentAndRetentionDays(boolean learningConsent) throws Exception {
		given(updateDataSettingsUseCase.updateDataSettings(any()))
				.willReturn(new DataSettingsResult(learningConsent, RETENTION_DAYS));

		mockMvc.perform(dataSettingsRequest(GUARDIAN, learningConsent))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"learningConsent":%s,"retentionDays":90}
						""".formatted(learningConsent), JsonCompareMode.STRICT));

		then(updateDataSettingsUseCase).should().updateDataSettings(new DataSettingsCommand(GUARDIAN, learningConsent));
	}

	@Test
	@DisplayName("PUT /v1/guardians/me/data-settings를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void updateDataSettingsWithCoGuardianTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(dataSettingsRequest(CO_GUARDIAN, true))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(updateDataSettingsUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder dataSettingsRequest(AuthPrincipal principal, boolean learningConsent) {
		return put(DATA_SETTINGS_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"learningConsent":%s}
						""".formatted(learningConsent));
	}

	private String bearerOf(AuthPrincipal principal) {
		return bearer(accessTokenFixture.user(principal.id(), principal.familyId(), principal.role()));
	}

	private static AuthPrincipal user(long userId, Role role) {
		return new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
	}

}

package com.example.pickupservice.common.security;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class AuthPrincipalTest extends IntegrationTestSupport {

	private static final String PRINCIPAL_PATH = "/test/principal/me";
	private static final long VEHICLE_ID = 77L;
	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;
	private static final long BIGINT_USER_ID = 5_000_000_001L;
	private static final long BIGINT_FAMILY_ID = 4_000_000_002L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("VEHICLE access 토큰(vehicleId 77 · familyId 12)으로 부르면 컨트롤러가 @AuthenticationPrincipal로 받은 AuthPrincipal은 type VEHICLE · id 77 · familyId 12 · role null이다")
	void vehicleTokenIsResolvedToVehicleAuthPrincipalWithoutRole() throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);

		mockMvc.perform(get(PRINCIPAL_PATH).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("VEHICLE"))
				.andExpect(jsonPath("$.id").value(VEHICLE_ID))
				.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
				.andExpect(jsonPath("$", hasKey("role")))
				.andExpect(jsonPath("$.role").value(nullValue()));
	}

	@ParameterizedTest(name = "role {0}")
	@EnumSource(Role.class)
	@DisplayName("USER access 토큰(userId 101 · familyId 12 · role 지정)으로 부르면 컨트롤러가 @AuthenticationPrincipal로 받은 AuthPrincipal은 type USER · id 101 · familyId 12 · 토큰의 role이다")
	void userTokenIsResolvedToUserAuthPrincipalWithTokenRole(Role role) throws Exception {
		String userToken = accessTokenFixture.user(USER_ID, FAMILY_ID, role);

		mockMvc.perform(get(PRINCIPAL_PATH).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("USER"))
				.andExpect(jsonPath("$.id").value(USER_ID))
				.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
				.andExpect(jsonPath("$.role").value(role.name()));
	}

	@Test
	@DisplayName("USER access 토큰의 userId 5000000001 · familyId 4000000002처럼 int 범위를 넘는 BIGINT 값도 AuthPrincipal의 id · familyId에 잘리지 않고 그대로 들어간다")
	void bigintIdsInTokenAreResolvedToAuthPrincipalWithoutTruncation() throws Exception {
		String userToken = accessTokenFixture.user(BIGINT_USER_ID, BIGINT_FAMILY_ID, Role.CO_GUARDIAN);

		mockMvc.perform(get(PRINCIPAL_PATH).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("USER"))
				.andExpect(jsonPath("$.id").value(BIGINT_USER_ID))
				.andExpect(jsonPath("$.familyId").value(BIGINT_FAMILY_ID))
				.andExpect(jsonPath("$.role").value("CO_GUARDIAN"));
	}

	@Test
	@DisplayName("familyId 클레임 없이 sub 77 · principalType VEHICLE만 담은 access 토큰으로 부르면 401이 아니라 200이고 AuthPrincipal은 type VEHICLE · id 77 · familyId null이다")
	void vehicleTokenWithoutFamilyIdClaimIsResolvedWithNullFamilyId() throws Exception {
		String vehicleTokenWithoutFamilyId = accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(VEHICLE_ID),
				"principalType", PrincipalType.VEHICLE.name()));

		mockMvc.perform(get(PRINCIPAL_PATH).header(HttpHeaders.AUTHORIZATION, bearer(vehicleTokenWithoutFamilyId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("VEHICLE"))
				.andExpect(jsonPath("$.id").value(VEHICLE_ID))
				.andExpect(jsonPath("$", hasKey("familyId")))
				.andExpect(jsonPath("$.familyId").value(nullValue()));
	}

	record PrincipalView(PrincipalType type, long id, Long familyId, Role role) {

		static PrincipalView from(AuthPrincipal principal) {
			return new PrincipalView(principal.type(), principal.id(), principal.familyId(), principal.role());
		}

	}

	@RestController
	static class PrincipalTestController {

		@GetMapping(PRINCIPAL_PATH)
		public PrincipalView me(@AuthenticationPrincipal AuthPrincipal principal) {
			return PrincipalView.from(principal);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(PrincipalTestController.class)
	static class PrincipalTestEndpointConfiguration {
	}

}

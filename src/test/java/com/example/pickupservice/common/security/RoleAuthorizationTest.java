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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class RoleAuthorizationTest extends IntegrationTestSupport {

	private static final String GUARDIAN_ONLY_PATH = "/test/role/guardian-only";
	private static final String VEHICLE_ONLY_PATH = "/test/role/vehicle-only";
	private static final String GUARDIAN_OK = "guardian-ok";
	private static final String VEHICLE_OK = "vehicle-ok";
	private static final long USER_ID = 101L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@ParameterizedTest(name = "role {0}")
	@EnumSource(value = Role.class, mode = EnumSource.Mode.EXCLUDE, names = "GUARDIAN")
	@DisplayName("@PreAuthorize(\"hasRole('GUARDIAN')\") API를 GUARDIAN이 아닌 role(CO_GUARDIAN · DELEGATE · TEACHER · CHILD)의 USER 토큰으로 부르면 403과 error.code FORBIDDEN 공통 오류 본문을 돌려준다")
	void nonGuardianRoleTokenOnGuardianOnlyApiReturns403WithForbiddenError(Role role) throws Exception {
		String userToken = accessTokenFixture.user(USER_ID, FAMILY_ID, role);

		mockMvc.perform(get(GUARDIAN_ONLY_PATH).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
				.andExpect(forbiddenError());
	}

	@Test
	@DisplayName("@PreAuthorize(\"hasRole('GUARDIAN')\") API를 GUARDIAN 토큰으로 부르면 권한 검사를 통과해 200과 컨트롤러 응답 guardian-ok를 돌려준다")
	void guardianTokenOnGuardianOnlyApiReachesController() throws Exception {
		String guardianToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(get(GUARDIAN_ONLY_PATH).header(HttpHeaders.AUTHORIZATION, bearer(guardianToken)))
				.andExpect(status().isOk())
				.andExpect(content().string(GUARDIAN_OK));
	}

	@Test
	@DisplayName("@PreAuthorize(\"hasRole('GUARDIAN')\") API를 role이 없는 VEHICLE 토큰으로 부르면 403과 error.code FORBIDDEN 공통 오류 본문을 돌려준다")
	void vehicleTokenOnGuardianOnlyApiReturns403WithForbiddenError() throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);

		mockMvc.perform(get(GUARDIAN_ONLY_PATH).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
				.andExpect(forbiddenError());
	}

	@Test
	@DisplayName("@PreAuthorize(\"hasAuthority('PRINCIPAL_VEHICLE')\") API를 GUARDIAN USER 토큰으로 부르면 403과 error.code FORBIDDEN 공통 오류 본문을 돌려준다")
	void userTokenOnVehicleOnlyApiReturns403WithForbiddenError() throws Exception {
		String guardianToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(get(VEHICLE_ONLY_PATH).header(HttpHeaders.AUTHORIZATION, bearer(guardianToken)))
				.andExpect(forbiddenError());
	}

	@Test
	@DisplayName("@PreAuthorize(\"hasAuthority('PRINCIPAL_VEHICLE')\") API를 VEHICLE 토큰으로 부르면 권한 검사를 통과해 200과 컨트롤러 응답 vehicle-ok를 돌려준다")
	void vehicleTokenOnVehicleOnlyApiReachesController() throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);

		mockMvc.perform(get(VEHICLE_ONLY_PATH).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
				.andExpect(status().isOk())
				.andExpect(content().string(VEHICLE_OK));
	}

	private static ResultMatcher forbiddenError() {
		return commonError(HttpStatus.FORBIDDEN, "FORBIDDEN");
	}

	@RestController
	static class RoleTestController {

		@PreAuthorize("hasRole('GUARDIAN')")
		@GetMapping(GUARDIAN_ONLY_PATH)
		public String guardianOnly() {
			return GUARDIAN_OK;
		}

		@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
		@GetMapping(VEHICLE_ONLY_PATH)
		public String vehicleOnly() {
			return VEHICLE_OK;
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(RoleTestController.class)
	static class RoleTestEndpointConfiguration {
	}

}

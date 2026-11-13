package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CallModeResult;
import com.example.pickupservice.user.application.port.in.FamilyResult;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.JoinCallMode;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class UserIdempotencyKeyTest extends FamilyWebTestSupport {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CHILD_USER_ID = 131L;
	private static final long TEACHER_USER_ID = 141L;
	private static final long VEHICLE_ID = 77L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("Idempotency-Key는 POST /v1/families/me/members · DELETE /v1/families/me/members/{userId} · DELETE /v1/vehicle-links/{vehicleId}에만 요구된다")
	void idempotencyKeyIsRequiredOnlyForRegisterRemoveMemberAndUnlinkVehicle() {
		given(getMyFamilyUseCase.getMyFamily(any())).willReturn(new FamilyResult(FAMILY_ID, JoinCallMode.AUTO, null,
				List.of(new MemberResult(GUARDIAN_USER_ID, Role.GUARDIAN, "김보호", "+821012345678", DeviceStatus.CONNECTED, null))));
		given(updateMemberUseCase.updateMember(any())).willReturn(
				new MemberResult(CHILD_USER_ID, Role.CHILD, "민준", null, DeviceStatus.CONNECTED, null));
		given(changeCallModeUseCase.changeCallMode(any())).willReturn(
				new CallModeResult(JoinCallMode.GUARDIAN_CONFIRM, 8));

		assertAll(
				() -> mockMvc.perform(withGuardian(post("/v1/families/me/members")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"role":"CHILD","name":"민준","phone":null}
										""")))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(delete("/v1/families/me/members/{userId}", TEACHER_USER_ID)))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(delete("/v1/vehicle-links/{vehicleId}", VEHICLE_ID)))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(get("/v1/families/me")))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withGuardian(patch("/v1/families/me/members/{userId}", CHILD_USER_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"name":"민준"}
										""")))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withGuardian(put("/v1/families/me/call-mode")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"joinCallMode":"GUARDIAN_CONFIRM"}
										""")))
						.andExpect(status().isOk()));

		then(registerMemberUseCase).shouldHaveNoInteractions();
		then(removeMemberUseCase).shouldHaveNoInteractions();
		then(unlinkVehicleUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder withGuardian(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION,
				bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN)));
	}

	private static ResultMatcher missingIdempotencyKey() {
		return result -> {
			status().isUnprocessableContent().match(result);
			commonErrorKeys().match(result);
			jsonPath("$.error.code").value("VALIDATION_FAILED").match(result);
			jsonPath("$.error.fields", hasSize(1)).match(result);
			jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER).match(result);
		};
	}

}

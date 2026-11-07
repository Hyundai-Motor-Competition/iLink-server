package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.UnlinkVehicleCommand;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class VehicleLinkControllerTest extends FamilyWebTestSupport {

	private static final String VEHICLE_LINK_PATH = "/v1/vehicle-links/{vehicleId}";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long VEHICLE_ID = 77L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("DELETE /v1/vehicle-links/{vehicleId}는 204를 돌려준다")
	void unlinkVehicleReturns204WithEmptyBody() throws Exception {
		mockMvc.perform(unlinkVehicleRequest(VEHICLE_ID, guardianBearer()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-links/{vehicleId}는 토큰의 familyId(12)와 경로의 vehicleId(77)를 UnlinkVehicleCommand로 유스케이스에 넘긴다")
	void unlinkVehiclePassesTokenFamilyIdAndPathVehicleIdToUseCase() throws Exception {
		mockMvc.perform(unlinkVehicleRequest(VEHICLE_ID, guardianBearer()))
				.andExpect(status().isNoContent());

		then(unlinkVehicleUseCase).should().unlinkVehicle(new UnlinkVehicleCommand(FAMILY_ID, VEHICLE_ID));
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-links/{vehicleId}를 CO_GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void unlinkVehicleWithCoGuardianTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(unlinkVehicleRequest(VEHICLE_ID,
						bearer(accessTokenFixture.user(CO_GUARDIAN_USER_ID, FAMILY_ID, Role.CO_GUARDIAN))))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(unlinkVehicleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-links/{vehicleId}가 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS를 돌려준다")
	void unlinkVehicleWithActivePickupReturns409ActivePickupExists() throws Exception {
		willThrow(BusinessException.of(
				HttpStatus.CONFLICT, "ACTIVE_PICKUP_EXISTS", "진행 중인 픽업이 있어 차량 연결을 해제할 수 없습니다"))
				.given(unlinkVehicleUseCase).unlinkVehicle(any());

		mockMvc.perform(unlinkVehicleRequest(VEHICLE_ID, guardianBearer()))
				.andExpect(commonError(HttpStatus.CONFLICT, "ACTIVE_PICKUP_EXISTS"));
	}

	private MockHttpServletRequestBuilder unlinkVehicleRequest(long vehicleId, String bearer) {
		return delete(VEHICLE_LINK_PATH, vehicleId)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
	}

	private String guardianBearer() {
		return bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN));
	}

}

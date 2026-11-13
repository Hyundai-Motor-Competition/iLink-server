package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.RegisterVehicleDeviceCommand;
import com.example.pickupservice.auth.application.port.in.VehicleDeviceResult;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class VehicleDeviceControllerTest extends AuthWebTestSupport {

	private static final String REGISTER_PATH = "/v1/vehicle-devices";
	private static final String SESSION_PATH = "/v1/vehicle-devices/me/session";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final String VALID_VIN = "KMHL14JA5PA123456";
	private static final String VIN_16_CHARS = "KMHL14JA5PA12345";
	private static final String VIN_WITH_I = "KMHL14JA5PI123456";
	private static final String MODEL = "아이오닉 5";
	private static final long DEVICE_ID = 301L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long USER_ID = 101L;
	private static final String DEVICE_TOKEN = "eyJ.vehicle-device-token";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("POST /v1/vehicle-devices는 인증 헤더 없이 201과 deviceId · vehicleId · deviceToken을 돌려준다")
	void registerVehicleDeviceWithoutAuthorizationHeaderReturns201() throws Exception {
		given(registerVehicleDeviceUseCase.registerVehicleDevice(new RegisterVehicleDeviceCommand(VALID_VIN, MODEL)))
				.willReturn(new VehicleDeviceResult(DEVICE_ID, VEHICLE_ID, DEVICE_TOKEN));

		mockMvc.perform(registerRequest(VALID_VIN, MODEL))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.deviceId").value(DEVICE_ID))
				.andExpect(jsonPath("$.vehicleId").value(VEHICLE_ID))
				.andExpect(jsonPath("$.deviceToken").value(DEVICE_TOKEN));
	}

	@Test
	@DisplayName("POST /v1/vehicle-devices는 본문의 vin · model을 RegisterVehicleDeviceCommand에 담아 유스케이스에 넘긴다")
	void registerVehicleDevicePassesVinAndModelToUseCase() throws Exception {
		given(registerVehicleDeviceUseCase.registerVehicleDevice(any()))
				.willReturn(new VehicleDeviceResult(DEVICE_ID, VEHICLE_ID, DEVICE_TOKEN));

		mockMvc.perform(registerRequest(VALID_VIN, MODEL))
				.andExpect(status().isCreated());

		then(registerVehicleDeviceUseCase).should()
				.registerVehicleDevice(new RegisterVehicleDeviceCommand(VALID_VIN, MODEL));
	}

	@Test
	@DisplayName("POST /v1/vehicle-devices에 16자 vin을 보내면 422 VALIDATION_FAILED를 돌려준다")
	void registerVehicleDeviceWith16CharVinReturns422() throws Exception {
		mockMvc.perform(registerRequest(VIN_16_CHARS, MODEL))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("vin"));

		then(registerVehicleDeviceUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-devices에 I가 들어 있는 17자 vin을 보내면 422 VALIDATION_FAILED와 fields의 vin을 돌려준다")
	void registerVehicleDeviceWithVinContainingIReturns422() throws Exception {
		mockMvc.perform(registerRequest(VIN_WITH_I, MODEL))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("vin"));

		then(registerVehicleDeviceUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-devices에 51자 model을 보내면 422 VALIDATION_FAILED와 fields의 model을 돌려준다")
	void registerVehicleDeviceWith51CharModelReturns422() throws Exception {
		mockMvc.perform(registerRequest(VALID_VIN, "가".repeat(51)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("model"));

		then(registerVehicleDeviceUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-devices가 연결된 VIN이면 409 VEHICLE_ALREADY_LINKED를 돌려준다")
	void registerVehicleDeviceWithLinkedVinReturns409() throws Exception {
		given(registerVehicleDeviceUseCase.registerVehicleDevice(any()))
				.willThrow(BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED));

		mockMvc.perform(registerRequest(VALID_VIN, MODEL))
				.andExpect(commonError(HttpStatus.CONFLICT, "VEHICLE_ALREADY_LINKED"));
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-devices/me/session은 204를 돌려준다")
	void logoutVehicleReturns204() throws Exception {
		mockMvc.perform(logoutRequest(vehicleTokenWithDeviceId(VEHICLE_ID, DEVICE_ID)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-devices/me/session은 access 토큰의 deviceId 클레임 값으로 LogoutVehicleUseCase.logoutVehicle을 부른다")
	void logoutVehiclePassesDeviceIdClaimToUseCase() throws Exception {
		mockMvc.perform(logoutRequest(vehicleTokenWithDeviceId(VEHICLE_ID, DEVICE_ID)))
				.andExpect(status().isNoContent());

		then(logoutVehicleUseCase).should().logoutVehicle(DEVICE_ID);
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-devices/me/session이 진행 중 픽업이 있으면 409 ACTIVE_PICKUP_EXISTS와 common ErrorCode 문구(차량 연결 해제와 같은 문구)를 돌려준다")
	void logoutVehicleWithActivePickupReturns409() throws Exception {
		willThrow(BusinessException.of(ErrorCode.ACTIVE_PICKUP_EXISTS))
				.given(logoutVehicleUseCase).logoutVehicle(anyLong());

		mockMvc.perform(logoutRequest(vehicleTokenWithDeviceId(VEHICLE_ID, DEVICE_ID)))
				.andExpect(commonError(HttpStatus.CONFLICT, "ACTIVE_PICKUP_EXISTS"))
				.andExpect(jsonPath("$.error.message").value("진행 중인 픽업이 있어 처리할 수 없습니다"));
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-devices/me/session을 USER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void logoutVehicleWithUserTokenReturns403() throws Exception {
		String userToken = accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(USER_ID),
				"principalType", PrincipalType.USER.name(),
				"familyId", FAMILY_ID,
				"role", Role.GUARDIAN.name(),
				"deviceId", DEVICE_ID));

		mockMvc.perform(logoutRequest(userToken))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(logoutVehicleUseCase).shouldHaveNoInteractions();
	}

	private String vehicleTokenWithDeviceId(long vehicleId, long deviceId) {
		return accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(vehicleId),
				"principalType", PrincipalType.VEHICLE.name(),
				"familyId", FAMILY_ID,
				"deviceId", deviceId));
	}

	private static MockHttpServletRequestBuilder registerRequest(String vin, String model) {
		return post(REGISTER_PATH)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"vin\": \"" + vin + "\", \"model\": \"" + model + "\"}");
	}

	private static MockHttpServletRequestBuilder logoutRequest(String accessToken) {
		return delete(SESSION_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken));
	}

}

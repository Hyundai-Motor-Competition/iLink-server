package com.example.pickupservice.auth.adapter.in.web.doc;

import com.example.pickupservice.auth.adapter.in.web.RegisterVehicleDeviceRequest;
import com.example.pickupservice.auth.adapter.in.web.VehicleDeviceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "차량 기기", description = "차량 기기 등록 · 로그아웃")
public interface VehicleDeviceApiDoc {

	@Operation(summary = "차량 기기 등록 (AUTH-02)", description = "인증 없이 부른다. 연결 전 기기 토큰(deviceToken)을 준다. 이 토큰으로는 LINK-01 · LINK-02만 부를 수 있다")
	@SecurityRequirements
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "deviceId · vehicleId · deviceToken")
	@ApiResponse(responseCode = "409", description = "VEHICLE_ALREADY_LINKED (이미 보호자가 연결된 VIN) · IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (vin 17자 영대문자 · 숫자, I · O · Q 제외 / model 1~50자 / Idempotency-Key 없음)")
	VehicleDeviceResponse registerVehicleDevice(RegisterVehicleDeviceRequest request);

	@Operation(summary = "차량 로그아웃 (AUTH-04)", description = "VEHICLE 토큰만. 지금 차량 기기를 폐기한다. 이미 받은 access 토큰은 만료될 때까지 쓸 수 있고 refresh만 막힌다")
	@ApiResponse(responseCode = "204", description = "폐기됨")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (USER 토큰)")
	@ApiResponse(responseCode = "409", description = "ACTIVE_PICKUP_EXISTS (진행 중 픽업이 있음)")
	void logoutVehicle();

}

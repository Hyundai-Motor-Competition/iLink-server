package com.example.pickupservice.user.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "차량 연결", description = "차량 연결 해제")
public interface VehicleLinkApiDoc {

	@Operation(summary = "차량 연결 해제 (LINK-05)", description = "GUARDIAN만. 행은 지우지 않고 unlinkedAt만 채운다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "204", description = "해제됨")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없거나 다른 가족의 차량)")
	@ApiResponse(responseCode = "409", description = "ACTIVE_PICKUP_EXISTS · IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음)")
	void unlinkVehicle(@Parameter(hidden = true) AuthPrincipal principal, long vehicleId);

}

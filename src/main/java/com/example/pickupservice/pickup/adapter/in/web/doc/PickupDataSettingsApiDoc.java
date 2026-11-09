package com.example.pickupservice.pickup.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.DataSettingsResponse;
import com.example.pickupservice.pickup.adapter.in.web.UpdateDataSettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "픽업 데이터 설정", description = "보호자의 학습 동의 · 보관 기간")
public interface PickupDataSettingsApiDoc {

	@Operation(summary = "데이터 설정 변경 (OUT-04)",
			description = "GUARDIAN. learningConsent를 바꾸고 retentionDays(90)를 돌려준다. "
					+ "false로 바꾸면 분석용 기록을 바로 파기 대기로 돌린다")
	@ApiResponse(responseCode = "200", description = "learningConsent · retentionDays")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (JSON 형식 오류)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN 토큰이 아님)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (입력값 오류)")
	ResponseEntity<DataSettingsResponse> updateDataSettings(@Parameter(hidden = true) AuthPrincipal principal,
			UpdateDataSettingsRequest request);

}

package com.example.pickupservice.pickup.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.CorrectOutcomeRequest;
import com.example.pickupservice.pickup.adapter.in.web.OutcomeResponse;
import com.example.pickupservice.pickup.adapter.in.web.PickupHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "픽업 결과 · 기록", description = "픽업 이력 · 결과 조회 · 결과 정정")
public interface PickupOutcomeApiDoc {

	@Operation(summary = "픽업 이력 (OUT-01)",
			description = "GUARDIAN · CO_GUARDIAN. 자녀가 포함된 세션 중 한국 기준 그 달의 COMPLETED · CANCELLED 세션을 날짜별로 돌려준다")
	@ApiResponse(responseCode = "200", description = "items[](date · sessionId · status · completedAt)")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (쿼리 month 없음)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN · CO_GUARDIAN 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (자녀가 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (month가 YYYY-MM 형식이 아님)")
	ResponseEntity<PickupHistoryResponse> history(@Parameter(hidden = true) AuthPrincipal principal,
			long childId,
			@Parameter(description = "조회할 달 (YYYY-MM, 한국 기준)", example = "2026-11") String month);

	@Operation(summary = "픽업 결과 (OUT-02)",
			description = "GUARDIAN · CO_GUARDIAN. 완료된 세션의 정차 예측 · 실제 시각과 차이, 정정 상태를 돌려준다")
	@ApiResponse(responseCode = "200", description = "sessionId · completedAt · spotName · boardingMethod · "
			+ "predictedStopAt · actualStopAt · diffSeconds · correction{status, correctedStopAt}")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN · CO_GUARDIAN 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것, 아직 결과가 없음)")
	ResponseEntity<OutcomeResponse> getOutcome(@Parameter(hidden = true) AuthPrincipal principal, long sessionId);

	@Operation(summary = "픽업 결과 정정 (OUT-03)",
			description = "GUARDIAN · CO_GUARDIAN. correctedStopAt(세션 날짜 안) · reasonCode(WRONG_STOP_TIME · OTHER) · "
					+ "note(0~200자)로 정정을 요청한다. actualStopAt 원본은 그대로 둔다")
	@ApiResponse(responseCode = "200", description = "OUT-02와 같은 모양 (correction.status REQUESTED)")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (JSON 형식 오류)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN · CO_GUARDIAN 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것, 아직 결과가 없음)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (입력값 오류)")
	ResponseEntity<OutcomeResponse> correct(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, CorrectOutcomeRequest request);

}

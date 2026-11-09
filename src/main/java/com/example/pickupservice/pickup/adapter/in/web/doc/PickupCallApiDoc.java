package com.example.pickupservice.pickup.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.AcknowledgeCallResponse;
import com.example.pickupservice.pickup.adapter.in.web.HoldCallRequest;
import com.example.pickupservice.pickup.adapter.in.web.RevokeCallRequest;
import com.example.pickupservice.pickup.adapter.in.web.SendCallRequest;
import com.example.pickupservice.pickup.adapter.in.web.SendCallResponse;
import com.example.pickupservice.pickup.adapter.in.web.SessionViewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "합류 호출", description = "합류 호출 보류 · 보내기 · 철회 · 확인")
public interface PickupCallApiDoc {

	@Operation(summary = "합류 안내 보내기 (CALL-02)",
			description = "차량 앱. 보호자 확인형 가족에서 stateVersion으로 합류 안내를 직접 보낸다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "callId · status")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "NOT_GUARDIAN_CONFIRM_MODE (AUTO 가족) · "
			+ "CALL_ALREADY_SENT (이미 보냄) · STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "CALL_GUARD_UNMET (error.unmet 함께) · "
			+ "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SendCallResponse> send(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, SendCallRequest request);

	@Operation(summary = "합류 호출 철회 (CALL-03)",
			description = "차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE. stateVersion · reason(0~100자)으로 보낸 호출을 철회한다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (status.call REVOKED)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (호출이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "CALL_ALREADY_REVOKED (이미 철회된 호출) · "
			+ "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SessionViewResponse> revoke(@Parameter(hidden = true) AuthPrincipal principal,
			long callId, RevokeCallRequest request);

	@Operation(summary = "합류 호출 확인 (CALL-04)",
			description = "자녀 앱. 자녀가 합류 안내를 확인한다. 여러 번 눌러도 첫 확인 시각을 유지한다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "callId · status · acknowledgedAt")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (자녀 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (호출이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "CALL_ALREADY_REVOKED (철회된 호출)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음)")
	ResponseEntity<AcknowledgeCallResponse> acknowledge(@Parameter(hidden = true) AuthPrincipal principal,
			long callId);

	@Operation(summary = "합류 호출 보류 (CALL-01 PUT)",
			description = "차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE. stateVersion으로 발송 전 합류 호출을 보류한다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (call.held true)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "CALL_ALREADY_SENT (이미 보냄, CALL-03으로 철회) · "
			+ "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SessionViewResponse> holdCall(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, HoldCallRequest request);

	@Operation(summary = "합류 호출 보류 해제 (CALL-01 DELETE)",
			description = "차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE. 쿼리 stateVersion으로 보류를 풀고 최신 ETA로 바로 다시 판정한다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (call.held false)")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (쿼리 stateVersion 없음)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음)")
	ResponseEntity<SessionViewResponse> releaseCallHold(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, int stateVersion);

}

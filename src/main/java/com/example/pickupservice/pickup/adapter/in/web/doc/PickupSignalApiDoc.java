package com.example.pickupservice.pickup.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.ChildLocationRequest;
import com.example.pickupservice.pickup.adapter.in.web.ChildReadyResponse;
import com.example.pickupservice.pickup.adapter.in.web.HelpRequestRequest;
import com.example.pickupservice.pickup.adapter.in.web.HelpRequestResponse;
import com.example.pickupservice.pickup.adapter.in.web.PrepExtensionResponse;
import com.example.pickupservice.pickup.adapter.in.web.PrepTimeExtensionRequest;
import com.example.pickupservice.pickup.adapter.in.web.VehicleEventRequest;
import com.example.pickupservice.pickup.adapter.in.web.VehicleTelemetryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "픽업 신호", description = "차량 ETA · 차량 이벤트 · 자녀 신호")
public interface PickupSignalApiDoc {

	@Operation(summary = "차량 ETA 보내기 (SIG-01)",
			description = "차량 앱(백그라운드, 15초마다). observedAt · navEtaSeconds(0 이상) · remainingMeters(0 이상) · "
					+ "location{lat, lng} · headingDeg(0~359, 선택) · speedKmh(0 이상, 선택) · gear(P · R · N · D, 선택). "
					+ "Idempotency-Key는 받지 않는다")
	@ApiResponse(responseCode = "202", description = "본문 없음")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (JSON 형식 오류)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (입력값 오류)")
	ResponseEntity<Void> receiveTelemetry(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, VehicleTelemetryRequest request);

	@Operation(summary = "차량 이벤트 보내기 (SIG-02)",
			description = "차량 앱(백그라운드, 즉시). eventId(UUID) · type(STOPPED · DEPARTED · STOP_FAILED · DOOR_CYCLE) · "
					+ "occurredAt · location · door(DOOR_CYCLE이면 필수: FRONT_RIGHT · REAR_LEFT · REAR_RIGHT). "
					+ "Idempotency-Key 대신 eventId로 중복을 거른다")
	@ApiResponse(responseCode = "202", description = "본문 없음")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (JSON 형식 오류)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (입력값 오류 · DOOR_CYCLE에 door 없음 · 운전석 문)")
	ResponseEntity<Void> receiveVehicleEvent(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, VehicleEventRequest request);

	@Operation(summary = "준비 시간 연장 (SIG-03)",
			description = "자녀 앱. minutes(5 · 10 · 15)만큼 준비 시간을 늘린다. 한 세션에 누적 15분까지")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "extraPrepMinutes · callNotBefore")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (자녀 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "CALL_ALREADY_SENT (합류 안내를 이미 받음) · "
			+ "SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션)")
	@ApiResponse(responseCode = "422", description = "PREP_EXTENSION_LIMIT (누적 15분 초과) · "
			+ "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<PrepExtensionResponse> extendPrepTime(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, PrepTimeExtensionRequest request);

	@Operation(summary = "자녀 위치 보내기 (SIG-04)",
			description = "자녀 앱. at · lat · lng · accuracyM(선택). Idempotency-Key는 받지 않는다")
	@ApiResponse(responseCode = "202", description = "본문 없음")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (JSON 형식 오류)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (자녀 토큰이 아님) · "
			+ "LOCATION_CONSENT_REQUIRED (위치 동의 없음)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션) · "
			+ "CHILD_NOT_CALLED (합류 호출을 받은 자녀가 아님)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (입력값 오류)")
	ResponseEntity<Void> reportChildLocation(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, ChildLocationRequest request);

	@Operation(summary = "도움 요청 (SIG-05)",
			description = "자녀 앱. type으로 도움을 요청한다. 1분 안에 다시 요청하면 429")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "nextAllowedAt")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (자녀 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	@ApiResponse(responseCode = "429", description = "HELP_REQUEST_TOO_SOON (retryAfterSeconds 포함, error.nextAllowedAt 함께)")
	ResponseEntity<HelpRequestResponse> requestHelp(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, HelpRequestRequest request);

	@Operation(summary = "준비 완료 (SIG-06)",
			description = "자녀 앱. 본문 없음. 처음 누를 때만 readyAt을 기록하고 보호자 · 차량에 CHILD_READY를 알린다. "
					+ "다시 눌러도 첫 readyAt을 돌려준다. Idempotency-Key는 받지 않는다")
	@ApiResponse(responseCode = "200", description = "childUserId · readyAt")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (자녀 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것 · 세션 자녀가 아님)")
	@ApiResponse(responseCode = "409", description = "SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션)")
	ResponseEntity<ChildReadyResponse> markReady(@Parameter(hidden = true) AuthPrincipal principal, long sessionId);

}

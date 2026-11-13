package com.example.pickupservice.pickup.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.ActiveSessionResponse;
import com.example.pickupservice.pickup.adapter.in.web.CancelSessionRequest;
import com.example.pickupservice.pickup.adapter.in.web.ChangeSpotRequest;
import com.example.pickupservice.pickup.adapter.in.web.CompletePickupRequest;
import com.example.pickupservice.pickup.adapter.in.web.ConfirmRouteRequest;
import com.example.pickupservice.pickup.adapter.in.web.ContactsResponse;
import com.example.pickupservice.pickup.adapter.in.web.HoldSessionRequest;
import com.example.pickupservice.pickup.adapter.in.web.OpenIncidentRequest;
import com.example.pickupservice.pickup.adapter.in.web.RejectBoardingRequest;
import com.example.pickupservice.pickup.adapter.in.web.SessionEventPageResponse;
import com.example.pickupservice.pickup.adapter.in.web.SessionViewResponse;
import com.example.pickupservice.pickup.adapter.in.web.StartPickupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "픽업 세션", description = "픽업 세션 시작 · 조회 · 경로 확정 · 장소 변경 · 보류 · 취소 · 탑승 완료 · 탑승 취소 · 탑승 예외")
public interface PickupSessionApiDoc {

	@Operation(summary = "진행 중 세션 조회 (SES-01)",
			description = "모든 앱. 차량은 그 차량의 세션, 사용자는 자기 가족 세션, 자녀는 자기가 포함된 세션을 돌려준다")
	@ApiResponse(responseCode = "200", description = "sessionId · status(ACTIVE · ON_HOLD) · stateVersion")
	@ApiResponse(responseCode = "204", description = "진행 중 세션 없음")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	ResponseEntity<ActiveSessionResponse> findActive(@Parameter(hidden = true) AuthPrincipal principal);

	@Operation(summary = "픽업 세션 시작 (SES-02)",
			description = "차량 토큰. scheduleIds(1개 이상) · date(한국 날짜)로 세션을 시작한다. "
					+ "진행 중인 같은 세션이면 그 세션을 200으로 돌려준다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "새 세션. SessionView")
	@ApiResponse(responseCode = "200", description = "진행 중인 같은 세션. SessionView")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "409", description = "ANOTHER_SESSION_ACTIVE (이 차량에 다른 진행 중 세션이 있음)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음) · "
			+ "NOT_TODAY (date가 한국 날짜로 오늘이 아님) · "
			+ "SCHEDULES_NOT_GROUPABLE (같은 날 · 같은 학원 · 같은 시간대 일정이 아님) · "
			+ "SETUP_INCOMPLETE (기본 장소 또는 자녀 기기 없음, missing 함께)")
	ResponseEntity<SessionViewResponse> start(@Parameter(hidden = true) AuthPrincipal principal,
			StartPickupRequest request);

	@Operation(summary = "세션 조회 (SES-03)",
			description = "그 세션의 차량, 가족 사용자. 주체마다 보이는 필드가 다르다. "
					+ "vehicleLocation(마지막 차량 위치)은 차량 · 보호자에게 늘, 자녀에게는 남은 거리 700m 이하 또는 "
					+ "남은 시간 180초 이하일 때만 주고, 끝난 세션이면 null이다. 앱은 화면이 켜진 동안 15초마다 조회한다")
	@ApiResponse(responseCode = "200", description = "SessionView(주체별 필드)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	ResponseEntity<SessionViewResponse> getSession(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId);

	@Operation(summary = "경로 확정 (SES-04)",
			description = "차량 토큰. 내비 SDK가 길안내를 시작한 뒤 spotId · navRouteId · stateVersion으로 경로를 확정한다. "
					+ "이때부터 자녀 화면에 장소가 보인다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "STATE_VERSION_CONFLICT (stateVersion이 다르거나 spotId가 세션 장소와 다름, "
			+ "최신 session 함께) · SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음) · "
			+ "SPOT_NOT_SAVED (가족이 저장한 장소가 아님)")
	ResponseEntity<SessionViewResponse> confirmRoute(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, ConfirmRouteRequest request);

	@Operation(summary = "장소 변경 (SES-05)",
			description = "차량 앱, GUARDIAN · CO_GUARDIAN. spotId · stateVersion · spotVersion으로 저장된 다른 장소로 바꾼다. "
					+ "차량은 다시 길안내한 뒤 경로 확정(SES-04)을 부른다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (DELEGATE · TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "CHILD_ALREADY_MOVING (자녀가 이동 중) · "
			+ "CALL_ALREADY_SENT (합류 안내를 이미 보냄, 먼저 철회) · STATE_VERSION_CONFLICT (최신 SessionView 함께) · "
			+ "SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SessionViewResponse> changeSpot(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, ChangeSpotRequest request);

	@Operation(summary = "세션 보류 (SES-06)",
			description = "차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE. stateVersion · reason(0~100자)으로 세션을 ON_HOLD로 바꾼다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (status.session ON_HOLD)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SessionViewResponse> hold(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, HoldSessionRequest request);

	@Operation(summary = "세션 보류 해제 (SES-06 DELETE)",
			description = "차량 앱, GUARDIAN · CO_GUARDIAN · DELEGATE. 쿼리 stateVersion으로 보류를 풀고 세션을 ACTIVE로 되돌린다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (status.session ACTIVE)")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (쿼리 stateVersion 없음)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음)")
	ResponseEntity<SessionViewResponse> releaseHold(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, int stateVersion);

	@Operation(summary = "픽업 취소 (SES-07)",
			description = "차량 앱, GUARDIAN · CO_GUARDIAN. stateVersion · reason(0~100자)으로 세션을 CANCELLED로 바꾼다. "
					+ "보낸 합류 안내는 자동 철회하고 자녀 위치를 비운다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (status.session CANCELLED)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (DELEGATE · TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션) · "
			+ "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SessionViewResponse> cancel(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, CancelSessionRequest request);

	@Operation(summary = "세션 이벤트 목록 (SES-09)",
			description = "그 세션의 차량, 가족 사용자. afterSeq보다 큰 seq를 오름차순으로 limit개(기본 100, 최대 100) 돌려준다. "
					+ "저장하지 않는 ETA_UPDATED · VEHICLE_LOCATION은 나오지 않는다")
	@ApiResponse(responseCode = "200", description = "items[](eventId · seq · type · stateVersion · occurredAt · payload), hasMore")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (쿼리 afterSeq 없음)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (limit이 1~100이 아님)")
	ResponseEntity<SessionEventPageResponse> listEvents(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, long afterSeq, int limit);

	@Operation(summary = "세션 연락처 (SES-10)",
			description = "그 세션의 차량, 가족 사용자. 가족 멤버의 전화번호와 학원 대표번호(role ACADEMY)를 돌려준다")
	@ApiResponse(responseCode = "200", description = "items[](name · role(GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · ACADEMY) · phone)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	ResponseEntity<ContactsResponse> getContacts(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId);

	@Operation(summary = "탑승 완료 (SES-11)",
			description = "차량 토큰. mode(IMMEDIATE · MANUAL) · stateVersion · notBoardedChildUserIds(선택)로 세션을 COMPLETED로 바꾼다. "
					+ "IMMEDIATE는 도어 인식 차량에서 10초를 기다리지 않고 바로 완료, MANUAL은 도어 신호 없는 차량 · 정차 중만")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (status.session COMPLETED)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류) · "
			+ "VEHICLE_NOT_STOPPED (MANUAL인데 정차 중이 아님) · "
			+ "CHILDREN_NOT_ALL_BOARDED (탑승하지도 미탑승 지정되지도 않은 자녀가 있음)")
	ResponseEntity<SessionViewResponse> complete(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, CompletePickupRequest request);

	@Operation(summary = "탑승 취소 (SES-12)",
			description = "차량 토큰. 「아이가 타지 않았어요」. childUserId(자녀가 여러 명이면 필수) · stateVersion으로 "
					+ "탑승 인식 뒤 10초 안에 그 자녀의 탑승 기록을 비운다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "REJECTION_WINDOW_CLOSED (탑승 인식 전이거나 10초가 지남, 예외 열기 SES-13 사용) · "
			+ "STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SessionViewResponse> rejectBoarding(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, RejectBoardingRequest request);

	@Operation(summary = "탑승 예외 열기 (SES-13)",
			description = "차량 토큰. type(CHILD_NOT_ON_BOARD) · childUserId · stateVersion으로 탑승 예외를 열고 세션을 ON_HOLD로 바꾼다. "
					+ "차량은 예외 · 복구, 보호자는 예외 · 권한 관리, 자녀는 안전 대기 화면으로 간다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "200", description = "SessionView (status.session ON_HOLD, incident 채워짐)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것, 세션에 없는 childUserId)")
	@ApiResponse(responseCode = "409", description = "INCIDENT_ALREADY_OPEN (이미 열린 예외가 있음) · "
			+ "SESSION_ALREADY_FINISHED (이미 완료 · 취소된 세션) · STATE_VERSION_CONFLICT (최신 SessionView 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음 · 입력값 오류)")
	ResponseEntity<SessionViewResponse> openIncident(@Parameter(hidden = true) AuthPrincipal principal,
			long sessionId, OpenIncidentRequest request);

}

package com.example.pickupservice.schedule.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.SetupCompletionRequest;
import com.example.pickupservice.schedule.adapter.in.web.SetupCompletionResponse;
import com.example.pickupservice.schedule.adapter.in.web.SetupSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "설정", description = "자녀 설정 요약 · 설정 완료")
public interface SetupApiDoc {

	@Operation(summary = "설정 요약 (SET-07)", description = "GUARDIAN · CO_GUARDIAN. 자녀 · 일정 · 픽업 장소 · 합류 통화 방식과 "
			+ "누락 항목, settingsVersion을 돌려준다")
	@ApiResponse(responseCode = "200", description = "child{userId, name, deviceStatus} · schedules[]{scheduleId, academyName, status} · "
			+ "pickupSpots[]{spotId, name, isDefault} · joinCallMode · missing[] · settingsVersion")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (내 가족의 CHILD가 아님)")
	SetupSummaryResponse getSetupSummary(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "자녀 userId", required = true) long childId);

	@Operation(summary = "설정 완료 (SET-08)", description = "GUARDIAN · CO_GUARDIAN. Idempotency-Key 필요. "
			+ "settingsVersion이 요약 때와 같고 누락이 없으면 완료 처리한다")
	@ApiResponse(responseCode = "200", description = "syncedToVehicle")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (내 가족의 CHILD가 아님)")
	@ApiResponse(responseCode = "409", description = "SETTINGS_VERSION_CONFLICT (요약 이후 설정이 바뀜)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (settingsVersion 필수 · Idempotency-Key 필수) · "
			+ "SETUP_INCOMPLETE (누락 항목 있음)")
	SetupCompletionResponse completeSetup(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "자녀 userId", required = true) long childId,
			SetupCompletionRequest request);

}

package com.example.pickupservice.schedule.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.VehicleSettingsSummaryResponse;
import com.example.pickupservice.schedule.adapter.in.web.WeeklyPickupsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "차량 일정", description = "차량용 주간 픽업 · 설정 요약")
public interface VehicleScheduleApiDoc {

	@Operation(summary = "주간 픽업 (SET-09)", description = "차량 토큰 전용. weekStart가 없으면 이번 주를 돌려준다")
	@ApiResponse(responseCode = "200", description = "weekStart · days[]{date, pickups[]{scheduleId, childName, "
			+ "profileImageUrl(없으면 null), academyName, classEndTime, targetPickupTime}} · "
			+ "readiness{startable, missing[](CHILD_DEVICE · SCHEDULE · PICKUP_SPOT)}")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST (weekStart가 yyyy-MM-dd 형식이 아님)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	WeeklyPickupsResponse getWeeklyPickups(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "주 시작일 (yyyy-MM-dd, 생략 가능)") LocalDate weekStart);

	@Operation(summary = "차량 설정 요약 (SET-10)", description = "차량 토큰 전용. 보호자 이름(가림) · 자녀 · 합류 통화 방식을 돌려준다")
	@ApiResponse(responseCode = "200", description = "guardianNameMasked · children[]{userId, name} · joinCallMode")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	VehicleSettingsSummaryResponse getSettingsSummary(@Parameter(hidden = true) AuthPrincipal principal);

}

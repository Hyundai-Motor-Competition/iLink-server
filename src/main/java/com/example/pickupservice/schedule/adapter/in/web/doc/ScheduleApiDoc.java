package com.example.pickupservice.schedule.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.AcademySearchResponse;
import com.example.pickupservice.schedule.adapter.in.web.ScheduleListResponse;
import com.example.pickupservice.schedule.adapter.in.web.ScheduleRequest;
import com.example.pickupservice.schedule.adapter.in.web.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "일정", description = "학원 검색 · 픽업 일정")
public interface ScheduleApiDoc {

	@Operation(summary = "학원 검색 (SET-01)", description = "GUARDIAN · CO_GUARDIAN. 외부 지도 API 결과를 저장하지 않고 그대로 돌려준다")
	@ApiResponse(responseCode = "200", description = "검색 결과 items[]")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (query 2~50자)")
	@ApiResponse(responseCode = "502", description = "MAP_API_UNAVAILABLE")
	AcademySearchResponse searchAcademies(@Parameter(description = "검색어 (2~50자)", required = true) String query);

	@Operation(summary = "일정 등록 (SET-02)", description = "GUARDIAN · CO_GUARDIAN. Idempotency-Key 필요. "
			+ "academy.entrance가 없으면 status DRAFT, 있으면 READY")
	@ApiResponse(responseCode = "201", description = "scheduleId · academyId · status")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (내 가족의 CHILD가 아님)")
	@ApiResponse(responseCode = "409", description = "SCHEDULE_OVERLAP (error.overlapScheduleIds 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (weekdays MON~SUN 1개 이상 · classEndTime HH:MM · "
			+ "targetPickup 0~60) · DAILY_SCHEDULE_LIMIT (하루 3개 초과)")
	ScheduleResponse createSchedule(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "자녀 userId", required = true) long childId,
			ScheduleRequest request);

	@Operation(summary = "일정 수정 (SET-03)", description = "GUARDIAN · CO_GUARDIAN. Idempotency-Key 불필요. "
			+ "본문은 SET-02와 같다. 진행 중 세션에는 적용되지 않고 다음 세션부터 적용된다")
	@ApiResponse(responseCode = "200", description = "scheduleId · academyId · status")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (내 가족의 일정이 아님)")
	@ApiResponse(responseCode = "409", description = "SCHEDULE_OVERLAP (error.overlapScheduleIds 함께)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (weekdays MON~SUN 1개 이상 · classEndTime HH:MM · "
			+ "targetPickup 0~60) · DAILY_SCHEDULE_LIMIT (하루 3개 초과)")
	ScheduleResponse updateSchedule(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "일정 ID", required = true) long scheduleId,
			ScheduleRequest request);

	@Operation(summary = "일정 목록 (SET-04)", description = "GUARDIAN · CO_GUARDIAN. 자녀의 일정 목록")
	@ApiResponse(responseCode = "200", description = "items[](scheduleId · academy{academyId, name} · weekdays · "
			+ "classEndTime · targetPickup · status)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (내 가족의 CHILD가 아님)")
	ScheduleListResponse listSchedules(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "자녀 userId", required = true) long childId);

}

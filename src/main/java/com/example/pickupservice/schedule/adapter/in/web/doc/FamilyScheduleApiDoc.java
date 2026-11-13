package com.example.pickupservice.schedule.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.TodayPickupsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "가족 일정", description = "부모 메인 오늘의 픽업")
public interface FamilyScheduleApiDoc {

	@Operation(summary = "오늘의 픽업 (SET-12)", description = "GUARDIAN · CO_GUARDIAN 전용. 한국 날짜 오늘 가족 자녀들의 픽업을 "
			+ "targetAt 오름차순(같으면 scheduleId)으로 돌려준다. DRAFT 일정도 넣는다")
	@ApiResponse(responseCode = "200", description = "items[]{scheduleId, childName, academyName, targetAt(KST +09:00), "
			+ "status(SCHEDULED · IN_PROGRESS · COMPLETED)}")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN · CO_GUARDIAN이 아님)")
	TodayPickupsResponse getTodayPickups(@Parameter(hidden = true) AuthPrincipal principal);

}

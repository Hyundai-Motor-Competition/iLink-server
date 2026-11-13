package com.example.pickupservice.schedule.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.NextPickupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "자녀 일정", description = "자녀 앱용 다음 픽업")
public interface ChildScheduleApiDoc {

	@Operation(summary = "다음 픽업 (SET-11)", description = "CHILD 전용. 지금 이후 가장 가까운 READY 일정의 픽업을 돌려준다")
	@ApiResponse(responseCode = "200", description = "academyName · spotName(기본 장소가 없으면 null) · targetAt(KST +09:00). "
			+ "남은 일정이 없으면 빈 본문")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (CHILD가 아님)")
	NextPickupResponse getNextPickup(@Parameter(hidden = true) AuthPrincipal principal);

}

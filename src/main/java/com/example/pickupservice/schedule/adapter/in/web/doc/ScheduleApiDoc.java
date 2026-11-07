package com.example.pickupservice.schedule.adapter.in.web.doc;

import com.example.pickupservice.schedule.adapter.in.web.AcademySearchResponse;
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

}

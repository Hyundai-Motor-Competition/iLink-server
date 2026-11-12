package com.example.pickupservice.place.adapter.in.web.doc;

import com.example.pickupservice.place.adapter.in.web.SpotRecommendationsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "픽업 장소 추천", description = "학원 근처 공영주차장 · 길가 정차 후보 추천")
public interface PickupSpotRecommendationApiDoc {

	@Operation(summary = "픽업 장소 추천 (SPOT-04)",
			description = "GUARDIAN · CO_GUARDIAN · DELEGATE 또는 차량 토큰. 학원 근처 공영주차장 · 길가 정차 후보를 순위대로 돌려준다. "
					+ "지원 지역 밖이면 supported = false와 빈 items[]")
	@ApiResponse(responseCode = "200", description = "supported, items[]{rank, kind, name, location, walkMeters, "
			+ "walkSeconds, driveSeconds, totalSeconds, cautions[]}")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (허용되지 않은 역할)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (academyId · origin 없음 또는 형식 오류, "
			+ "arriveAt이 형식 오류이거나 과거)")
	SpotRecommendationsResponse recommendSpots(
			@Parameter(description = "학원 id (필수)") String academyId,
			@Parameter(description = "출발 좌표 lat,lng (필수)") String origin,
			@Parameter(description = "도착 희망 시각 ISO-8601, 예 2026-11-13T22:00:00+09:00 (생략 가능)") String arriveAt);

}

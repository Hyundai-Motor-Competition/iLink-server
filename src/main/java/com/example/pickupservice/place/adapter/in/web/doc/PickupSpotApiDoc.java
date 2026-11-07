package com.example.pickupservice.place.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.place.adapter.in.web.AddSpotRequest;
import com.example.pickupservice.place.adapter.in.web.AddSpotResponse;
import com.example.pickupservice.place.adapter.in.web.PickupSpotsResponse;
import com.example.pickupservice.place.adapter.in.web.SpotResponse;
import com.example.pickupservice.place.adapter.in.web.UpdateSpotRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "픽업 장소", description = "학원별 픽업 장소 조회 · 추가 · 수정 · 삭제")
public interface PickupSpotApiDoc {

	@Operation(summary = "픽업 장소 조회 (SPOT-01)",
			description = "GUARDIAN · CO_GUARDIAN · DELEGATE 또는 차량 토큰. 내 가족의 장소를 학원별로 묶어 돌려준다. "
					+ "near · radiusM은 함께 보내야 한다")
	@ApiResponse(responseCode = "200", description = "academies[]{academyId, academyName, spots[]{spotId, name, "
			+ "stopPoint, meetingPoint, memo, isDefault, walkMinutes}}")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (허용되지 않은 역할)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (near · radiusM 중 하나만 보냄, "
			+ "radiusM이 50~3000 밖, near가 lat,lng 형식이 아님)")
	PickupSpotsResponse listSpots(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "학원 id (생략 가능)") Long academyId,
			@Parameter(description = "기준 좌표 lat,lng (radiusM과 함께)") String near,
			@Parameter(description = "반경 m 50~3000 (near와 함께)") Integer radiusM);

	@Operation(summary = "픽업 장소 추가 (SPOT-02)",
			description = "GUARDIAN · CO_GUARDIAN. 내 가족의 학원에 장소를 추가한다. 가족 · 학원별 첫 장소는 기본 장소가 되고, "
					+ "isDefault = true면 이전 기본 장소는 해제된다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "spotId · isDefault")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (허용되지 않은 역할)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (학원이 없거나 다른 가족의 학원)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (academyId · stopPoint · meetingPoint 없음, "
			+ "name 1~50자, memo 0~200자, Idempotency-Key 없음) · SPOT_TOO_FAR (정차 · 만남 지점이 학원에서 500m 넘음)")
	AddSpotResponse addSpot(@Parameter(hidden = true) AuthPrincipal principal, AddSpotRequest request);

	@Operation(summary = "픽업 장소 수정 (SPOT-03)",
			description = "GUARDIAN · CO_GUARDIAN. 보낸 필드만 바꾼다. isDefault = true면 같은 학원의 이전 기본 장소는 해제된다")
	@ApiResponse(responseCode = "200", description = "spotId · name · stopPoint · meetingPoint · memo · isDefault")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (허용되지 않은 역할)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (장소가 없거나 다른 가족의 장소)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (name 1~50자, memo 0~200자) · "
			+ "SPOT_TOO_FAR (정차 · 만남 지점이 학원에서 500m 넘음) · "
			+ "DEFAULT_SPOT_REQUIRED (기본 장소를 isDefault = false로 바꾸려 함)")
	SpotResponse updateSpot(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "장소 id") long spotId, UpdateSpotRequest request);

	@Operation(summary = "픽업 장소 삭제 (SPOT-03)",
			description = "GUARDIAN · CO_GUARDIAN. 장소를 soft delete 한다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "204", description = "본문 없음")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (허용되지 않은 역할)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (장소가 없거나 다른 가족의 장소)")
	@ApiResponse(responseCode = "409", description = "SPOT_IN_USE (진행 중 세션의 장소)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음) · "
			+ "DEFAULT_SPOT_REQUIRED (다른 장소가 남아 있는 기본 장소)")
	void deleteSpot(@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "장소 id") long spotId);

}

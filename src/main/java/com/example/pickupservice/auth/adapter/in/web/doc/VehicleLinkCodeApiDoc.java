package com.example.pickupservice.auth.adapter.in.web.doc;

import com.example.pickupservice.auth.adapter.in.web.ApproveLinkRequest;
import com.example.pickupservice.auth.adapter.in.web.LinkCodeInfoResponse;
import com.example.pickupservice.auth.adapter.in.web.LinkCodeResponse;
import com.example.pickupservice.auth.adapter.in.web.LinkStatusResponse;
import com.example.pickupservice.auth.adapter.in.web.VehicleLinkResponse;
import com.example.pickupservice.common.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "차량 연결 코드", description = "QR 연결 코드 발급 · 상태 조회 · 코드 확인")
public interface VehicleLinkCodeApiDoc {

	@Operation(summary = "연결 코드 발급 (LINK-01)", description = "VEHICLE 토큰만(연결 전 기기 토큰 가능). 8자 코드를 5분 동안 유효하게 만들고 같은 기기가 발급한 이전 ISSUED 코드는 EXPIRED로 바꾼다. qrPayload는 pickup://link?code={code}")
	@ApiResponse(responseCode = "201", description = "codeId · code · qrPayload · expiresAt · reissuesLeft")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (USER 토큰)")
	@ApiResponse(responseCode = "409", description = "VEHICLE_ALREADY_LINKED (연결 전 토큰인데 연결된 차량) · VEHICLE_UNLINKED (연결 해제된 차량, 앱을 다시 등록)")
	@ApiResponse(responseCode = "429", description = "LINK_CODE_REISSUE_LIMIT (연속 5번 넘게 발급, retryAfterSeconds 300 포함)")
	LinkCodeResponse issueLinkCode(@Parameter(hidden = true) AuthPrincipal principal);

	@Operation(summary = "연결 상태 조회 (LINK-02)", description = "코드를 발급한 기기의 토큰만. 승인 전 WAITING(pollAfterSeconds 2), 만료 EXPIRED, 승인되면 LINKED 또는 RELINKED와 차량 토큰을 한 번만 준다. 1초보다 자주 부르면 429")
	@ApiResponse(responseCode = "200", description = "status · vehicleId · familyId · accessToken · refreshToken · pollAfterSeconds")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "NOT_CODE_OWNER (다른 기기가 발급한 코드) · FORBIDDEN (USER 토큰)")
	@ApiResponse(responseCode = "404", description = "LINK_CODE_INVALID (없거나 토큰을 이미 내려준 코드)")
	@ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS (1초보다 자주 부름, retryAfterSeconds 1 포함)")
	LinkStatusResponse getLinkStatus(@Parameter(hidden = true) AuthPrincipal principal, long codeId);

	@Operation(summary = "연결 코드 확인 (LINK-03)", description = "인증 없이 부른다. 가족에 연결된 차량의 코드면 RELINK, 아니면 NEW. 연결 전 토큰으로 발급한 코드는 항상 NEW. 연결은 만들지 않는다")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "status · vehicleModel · plateNumberMasked · expiresAt")
	@ApiResponse(responseCode = "404", description = "LINK_CODE_INVALID (없는 코드)")
	@ApiResponse(responseCode = "410", description = "LINK_CODE_EXPIRED (만료 · 사용된 코드)")
	@ApiResponse(responseCode = "429", description = "LINK_CODE_ATTEMPT_LIMIT (같은 기기에서 5번 실패, 1분 동안, 풀릴 때까지 남은 retryAfterSeconds 포함)")
	LinkCodeInfoResponse checkLinkCode(String code, @Parameter(hidden = true) HttpServletRequest request);

	@Operation(summary = "연결 승인 (LINK-04)", description = "인증 없이 부른다. Idempotency-Key 필요. 코드를 다시 검증하고 NEW면 가족 · GUARDIAN을 만들어 차량을 잇고, RELINK면 기존 GUARDIAN에게 새 토큰을 주고 이전 기기를 폐기한다. mode NEW에는 name(1~20자) 필수. platform은 ANDROID · IOS")
	@SecurityRequirements
	@ApiResponse(responseCode = "201", description = "vehicleId · familyId · userId · accessToken · refreshToken")
	@ApiResponse(responseCode = "404", description = "LINK_CODE_INVALID (없는 코드)")
	@ApiResponse(responseCode = "409", description = "LINK_MODE_MISMATCH (mode가 코드의 실제 상태와 다름) · VEHICLE_ALREADY_LINKED (동시 승인 · 그 사이 다른 승인으로 연결됨)")
	@ApiResponse(responseCode = "410", description = "LINK_CODE_EXPIRED (만료 · 사용된 코드)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED · CONSENT_REQUIRED (TERMS · PRIVACY 누락)")
	VehicleLinkResponse approveLink(ApproveLinkRequest request);

	@Operation(summary = "연결 승인 · 프로필 사진 포함 (LINK-04)", description = "인증 없이 부른다. Idempotency-Key 필요. multipart/form-data. request 파트는 JSON 승인 본문과 같고, profileImage 파트(JPEG · PNG · WEBP, 5MB 이하)는 선택이다. 0바이트 파트는 사진 없음으로 본다")
	@SecurityRequirements
	@ApiResponse(responseCode = "201", description = "vehicleId · familyId · userId · accessToken · refreshToken")
	@ApiResponse(responseCode = "404", description = "LINK_CODE_INVALID (없는 코드)")
	@ApiResponse(responseCode = "409", description = "LINK_MODE_MISMATCH · VEHICLE_ALREADY_LINKED · IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "410", description = "LINK_CODE_EXPIRED (만료 · 사용된 코드)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED · CONSENT_REQUIRED · PROFILE_IMAGE_INVALID(이미지가 아님)")
	@ApiResponse(responseCode = "413", description = "PROFILE_IMAGE_TOO_LARGE (사진이 5MB 초과)")
	@ApiResponse(responseCode = "503", description = "STORAGE_UNAVAILABLE (사진 저장소 업로드 실패)")
	VehicleLinkResponse approveLinkWithProfileImage(
			@Parameter(description = "승인 본문(JSON)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) ApproveLinkRequest request,
			@Parameter(description = "프로필 사진(선택)") MultipartFile profileImage) throws IOException;

}

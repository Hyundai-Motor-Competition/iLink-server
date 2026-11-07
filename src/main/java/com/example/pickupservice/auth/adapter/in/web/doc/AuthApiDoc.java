package com.example.pickupservice.auth.adapter.in.web.doc;

import com.example.pickupservice.auth.adapter.in.web.RefreshTokenRequest;
import com.example.pickupservice.auth.adapter.in.web.TokenResponse;
import com.example.pickupservice.auth.adapter.in.web.UpdatePushTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증", description = "토큰 갱신 · 푸시 토큰 갱신")
public interface AuthApiDoc {

	@Operation(summary = "토큰 갱신 (AUTH-01)", description = "access 토큰 없이 refresh 토큰으로 부른다. 새 access · refresh 토큰을 주고 이전 refresh 토큰은 폐기한다")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "새 accessToken · refreshToken · accessTokenExpiresAt")
	@ApiResponse(responseCode = "401", description = "REFRESH_TOKEN_INVALID (만료 · 폐기 · 모르는 refresh 토큰)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (refreshToken 없음)")
	TokenResponse refresh(RefreshTokenRequest request);

	@Operation(summary = "푸시 토큰 갱신 (AUTH-03)", description = "USER · VEHICLE 토큰 모두. 지금 기기의 pushToken · pushUpdatedAt을 바꾼다. 앱 실행 때와 Firebase onNewToken 때 부른다")
	@ApiResponse(responseCode = "204", description = "갱신됨")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (platform ANDROID · IOS · VEHICLE / pushToken 1~255자)")
	void updatePushToken(UpdatePushTokenRequest request);

}

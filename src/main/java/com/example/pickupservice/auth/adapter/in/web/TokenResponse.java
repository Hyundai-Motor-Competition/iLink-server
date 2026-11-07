package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.TokenResult;
import com.example.pickupservice.common.time.TimeUtils;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		String accessTokenExpiresAt
) {

	public static TokenResponse from(TokenResult result) {
		return new TokenResponse(
				result.accessToken(),
				result.refreshToken(),
				TimeUtils.toKst(result.accessTokenExpiresAt()));
	}

}

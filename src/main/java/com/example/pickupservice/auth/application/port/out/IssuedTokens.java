package com.example.pickupservice.auth.application.port.out;

import java.time.Instant;

public record IssuedTokens(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
		String refreshTokenHash) {
}

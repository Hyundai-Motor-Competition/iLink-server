package com.example.pickupservice.auth.application.port.in;

import java.time.Instant;

public record TokenResult(String accessToken, String refreshToken, Instant accessTokenExpiresAt) {
}

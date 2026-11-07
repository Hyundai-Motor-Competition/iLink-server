package com.example.pickupservice.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
		@NotBlank String refreshToken
) {
}

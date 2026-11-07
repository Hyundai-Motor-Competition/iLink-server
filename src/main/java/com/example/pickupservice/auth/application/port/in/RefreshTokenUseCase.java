package com.example.pickupservice.auth.application.port.in;

public interface RefreshTokenUseCase {

	TokenResult refresh(String refreshToken);
}

package com.example.pickupservice.auth.application.port.in;

public interface CheckLinkCodeUseCase {

	LinkCodeInfo checkLinkCode(String code, String clientKey);
}

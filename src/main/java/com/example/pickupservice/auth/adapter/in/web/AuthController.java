package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.adapter.in.web.doc.AuthApiDoc;
import com.example.pickupservice.auth.application.port.in.RefreshTokenUseCase;
import com.example.pickupservice.auth.application.port.in.UpdatePushTokenUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApiDoc {

	private final RefreshTokenUseCase refreshTokenUseCase;
	private final UpdatePushTokenUseCase updatePushTokenUseCase;

	@Override
	@PostMapping("/v1/auth/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return TokenResponse.from(refreshTokenUseCase.refresh(request.refreshToken()));
	}

	@Override
	@PutMapping("/v1/devices/me/push-token")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updatePushToken(@Valid @RequestBody UpdatePushTokenRequest request) {
		updatePushTokenUseCase.updatePushToken(request.toCommand(CurrentDevice.id()));
	}

}

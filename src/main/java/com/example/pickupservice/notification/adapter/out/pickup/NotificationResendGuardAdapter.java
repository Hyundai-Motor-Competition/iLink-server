package com.example.pickupservice.notification.adapter.out.pickup;

import com.example.pickupservice.notification.application.port.out.LoadResendGuardPort;
import com.example.pickupservice.pickup.application.port.in.ResendGuardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationResendGuardAdapter implements LoadResendGuardPort {

	private final ResendGuardUseCase resendGuardUseCase;

	@Override
	public boolean isSessionEnded(long sessionId) {
		return resendGuardUseCase.isSessionEnded(sessionId);
	}

	@Override
	public boolean isCallRevoked(long callId) {
		return resendGuardUseCase.isCallRevoked(callId);
	}
}

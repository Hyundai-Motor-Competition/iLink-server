package com.example.pickupservice.pickup.application.port.in;

public interface ResendGuardUseCase {

	boolean isSessionEnded(long sessionId);

	boolean isCallRevoked(long callId);
}

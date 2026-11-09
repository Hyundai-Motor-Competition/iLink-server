package com.example.pickupservice.notification.application.port.out;

public interface LoadResendGuardPort {

	boolean isSessionEnded(long sessionId);

	boolean isCallRevoked(long callId);
}

package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.ResendGuardUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResendGuardService implements ResendGuardUseCase {

	private final LoadSessionPort loadSessionPort;
	private final LoadCallPort loadCallPort;

	@Override
	public boolean isSessionEnded(long sessionId) {
		return loadSessionPort.load(sessionId)
				.map(session -> session.getSessionStatus() == SessionStatus.COMPLETED
						|| session.getSessionStatus() == SessionStatus.CANCELLED)
				.orElse(true);
	}

	@Override
	public boolean isCallRevoked(long callId) {
		return loadCallPort.loadCall(callId)
				.map(call -> call.getStatus() == CallStatus.REVOKED)
				.orElse(true);
	}
}

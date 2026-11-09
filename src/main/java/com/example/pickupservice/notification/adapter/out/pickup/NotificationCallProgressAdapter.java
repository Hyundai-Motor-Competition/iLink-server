package com.example.pickupservice.notification.adapter.out.pickup;

import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.pickup.application.port.in.CallProgressUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NotificationCallProgressAdapter implements CallProgressPort {

	private final CallProgressUseCase callProgressUseCase;

	@Override
	public void markCallSent(long callId, Instant at) {
		callProgressUseCase.markCallSent(callId, at);
	}

	@Override
	public void markCallDelivered(long callId, Instant at) {
		callProgressUseCase.markCallDelivered(callId, at);
	}

	@Override
	public boolean isCallAcknowledged(long callId) {
		return callProgressUseCase.isCallAcknowledged(callId);
	}
}

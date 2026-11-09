package com.example.pickupservice.pickup.application.port.in;

import java.time.Instant;

public interface CallProgressUseCase {

	void markCallSent(long callId, Instant sentAt);

	void markCallDelivered(long callId, Instant deliveredAt);

	boolean isCallAcknowledged(long callId);

}

package com.example.pickupservice.notification.application.port.out;

import java.time.Instant;

public interface CallProgressPort {

	void markCallSent(long callId, Instant at);

	void markCallDelivered(long callId, Instant at);

	boolean isCallAcknowledged(long callId);
}

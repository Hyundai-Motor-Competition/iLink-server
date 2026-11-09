package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.pickup.domain.CallStatus;

import java.time.Instant;

public record AcknowledgeResult(
		long callId,
		CallStatus status,
		Instant acknowledgedAt
) {
}

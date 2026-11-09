package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.pickup.domain.CallStatus;

public record CallResult(
		long callId,
		CallStatus status
) {
}

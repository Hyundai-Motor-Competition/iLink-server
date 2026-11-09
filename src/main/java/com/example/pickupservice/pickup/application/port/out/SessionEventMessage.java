package com.example.pickupservice.pickup.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SessionEventMessage(
		long sessionId,
		Long eventId,
		Long seq,
		String type,
		int stateVersion,
		Instant occurredAt,
		Map<String, Object> payload,
		List<Long> receiverUserIds
) {
}

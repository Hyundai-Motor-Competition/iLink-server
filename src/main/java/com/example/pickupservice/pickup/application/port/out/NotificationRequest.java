package com.example.pickupservice.pickup.application.port.out;

import java.util.List;

public record NotificationRequest(
		long sessionId,
		Long callId,
		String type,
		List<Long> receiverUserIds
) {
}

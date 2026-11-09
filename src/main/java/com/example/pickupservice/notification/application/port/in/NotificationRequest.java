package com.example.pickupservice.notification.application.port.in;

import java.util.List;

public record NotificationRequest(
		long sessionId,
		Long callId,
		String type,
		List<Long> receiverUserIds
) {
}

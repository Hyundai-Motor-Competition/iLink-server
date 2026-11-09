package com.example.pickupservice.notification.application.port.in;

import java.time.Instant;

public record NotificationView(
		long notificationId,
		String type,
		String receiverName,
		String title,
		Instant sentAt,
		Instant deliveredAt,
		Instant openedAt,
		int attempt
) {
}

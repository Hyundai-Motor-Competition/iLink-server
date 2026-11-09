package com.example.pickupservice.notification.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

import java.time.Instant;

public record DeliveryReceiptCommand(
		AuthPrincipal principal,
		long notificationId,
		Instant receivedAt
) {
}

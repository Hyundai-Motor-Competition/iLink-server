package com.example.pickupservice.notification.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.notification.application.port.in.DeliveryReceiptCommand;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record DeliveryReceiptRequest(
		@NotNull OffsetDateTime receivedAt
) {

	public DeliveryReceiptCommand toCommand(AuthPrincipal principal, long notificationId) {
		return new DeliveryReceiptCommand(principal, notificationId, receivedAt.toInstant());
	}

}

package com.example.pickupservice.notification.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record ResendCommand(
		AuthPrincipal principal,
		long notificationId
) {
}

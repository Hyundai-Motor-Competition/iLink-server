package com.example.pickupservice.notification.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record NotificationListQuery(
		AuthPrincipal principal,
		long sessionId
) {
}

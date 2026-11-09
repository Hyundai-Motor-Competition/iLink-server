package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public record DataSettingsCommand(
		AuthPrincipal principal,
		boolean learningConsent
) {
}

package com.example.pickupservice.pickup.application.port.in;

public record DataSettingsResult(
		boolean learningConsent,
		int retentionDays
) {
}

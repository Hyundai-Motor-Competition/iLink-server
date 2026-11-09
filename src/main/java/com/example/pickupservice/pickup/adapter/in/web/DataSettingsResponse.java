package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.DataSettingsResult;

public record DataSettingsResponse(
		boolean learningConsent,
		int retentionDays
) {

	public static DataSettingsResponse from(DataSettingsResult result) {
		return new DataSettingsResponse(result.learningConsent(), result.retentionDays());
	}

}

package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.DataSettingsCommand;
import jakarta.validation.constraints.NotNull;

public record UpdateDataSettingsRequest(
		@NotNull Boolean learningConsent
) {

	public DataSettingsCommand toCommand(AuthPrincipal principal) {
		return new DataSettingsCommand(principal, learningConsent);
	}

}

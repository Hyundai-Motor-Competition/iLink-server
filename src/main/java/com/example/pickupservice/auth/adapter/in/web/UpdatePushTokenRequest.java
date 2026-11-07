package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.UpdatePushTokenCommand;
import com.example.pickupservice.auth.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePushTokenRequest(
		@NotNull Platform platform,
		@NotBlank @Size(max = 255) String pushToken
) {

	public UpdatePushTokenCommand toCommand(long deviceId) {
		return new UpdatePushTokenCommand(deviceId, platform, pushToken);
	}

}

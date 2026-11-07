package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.user.application.port.in.CallModeResult;
import com.example.pickupservice.user.domain.JoinCallMode;

public record CallModeResponse(
		JoinCallMode joinCallMode,
		int settingsVersion
) {

	public static CallModeResponse from(CallModeResult result) {
		return new CallModeResponse(result.joinCallMode(), result.settingsVersion());
	}

}

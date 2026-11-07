package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.user.domain.JoinCallMode;

public record CallModeResult(
		JoinCallMode joinCallMode,
		int settingsVersion
) {
}

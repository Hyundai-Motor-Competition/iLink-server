package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.user.domain.JoinCallMode;

public record ChangeCallModeCommand(
		long familyId,
		JoinCallMode joinCallMode
) {
}

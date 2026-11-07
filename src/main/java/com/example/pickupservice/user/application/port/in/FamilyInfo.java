package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.user.domain.JoinCallMode;

public record FamilyInfo(
		long familyId,
		JoinCallMode joinCallMode,
		int settingsVersion
) {
}

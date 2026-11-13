package com.example.pickupservice.user.application.port.in;

public record ChildLocationResult(
		boolean childLocationEnabled,
		int settingsVersion
) {
}

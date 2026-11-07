package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.schedule.domain.DeviceStatus;

public record SetupChild(
		long userId,
		String name,
		DeviceStatus deviceStatus
) {
}

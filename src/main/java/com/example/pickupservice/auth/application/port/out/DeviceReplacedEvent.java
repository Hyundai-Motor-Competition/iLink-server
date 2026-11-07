package com.example.pickupservice.auth.application.port.out;

import java.util.List;

public record DeviceReplacedEvent(List<Long> previousDeviceIds) {

	public DeviceReplacedEvent {
		previousDeviceIds = List.copyOf(previousDeviceIds);
	}
}

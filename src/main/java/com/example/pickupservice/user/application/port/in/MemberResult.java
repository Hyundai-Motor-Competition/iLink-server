package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.domain.DeviceStatus;

public record MemberResult(
		long userId,
		Role role,
		String name,
		String phone,
		DeviceStatus deviceStatus
) {
}

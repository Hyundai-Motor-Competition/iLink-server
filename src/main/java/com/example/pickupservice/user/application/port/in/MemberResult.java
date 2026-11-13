package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.domain.DeviceStatus;

public record MemberResult(
		long userId,
		Role role,
		String name,
		String phone,
		DeviceStatus deviceStatus,
		String profileImageUrl,
		String grade
) {

	public MemberResult(long userId, Role role, String name, String phone, DeviceStatus deviceStatus,
			String profileImageUrl) {
		this(userId, role, name, phone, deviceStatus, profileImageUrl, null);
	}

	public MemberResult withGrade(String grade) {
		return new MemberResult(userId, role, name, phone, deviceStatus, profileImageUrl, grade);
	}
}

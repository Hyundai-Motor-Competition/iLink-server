package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.security.Role;

public record MemberInfo(
		long userId,
		long familyId,
		Role role,
		String name,
		String phone
) {
}

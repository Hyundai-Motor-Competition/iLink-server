package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.security.Role;

public record RegisterMemberCommand(
		long familyId,
		Role requesterRole,
		Role role,
		String name,
		String phone
) {
}

package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.security.Role;

public record UpdateMemberCommand(
		long familyId,
		long requesterId,
		Role requesterRole,
		long userId,
		String name,
		String phone,
		Role role
) {
}

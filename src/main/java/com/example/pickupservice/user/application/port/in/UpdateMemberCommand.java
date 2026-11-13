package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.security.Role;

public record UpdateMemberCommand(
		long familyId,
		long requesterId,
		Role requesterRole,
		long userId,
		String name,
		String phone,
		Role role,
		String grade
) {

	public UpdateMemberCommand(long familyId, long requesterId, Role requesterRole, long userId, String name,
			String phone, Role role) {
		this(familyId, requesterId, requesterRole, userId, name, phone, role, null);
	}

	public UpdateMemberCommand withGrade(String grade) {
		return new UpdateMemberCommand(familyId, requesterId, requesterRole, userId, name, phone, role, grade);
	}
}

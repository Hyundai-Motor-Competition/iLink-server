package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.common.security.Role;

public record RegisterMemberCommand(
		long familyId,
		Role requesterRole,
		Role role,
		String name,
		String phone,
		ProfileImage profileImage,
		String grade
) {

	public RegisterMemberCommand(long familyId, Role requesterRole, Role role, String name, String phone) {
		this(familyId, requesterRole, role, name, phone, null);
	}

	public RegisterMemberCommand(long familyId, Role requesterRole, Role role, String name, String phone,
			ProfileImage profileImage) {
		this(familyId, requesterRole, role, name, phone, profileImage, null);
	}

	public RegisterMemberCommand withGrade(String grade) {
		return new RegisterMemberCommand(familyId, requesterRole, role, name, phone, profileImage, grade);
	}

	public RegisterMemberCommand withProfileImage(ProfileImage profileImage) {
		return new RegisterMemberCommand(familyId, requesterRole, role, name, phone, profileImage, grade);
	}
}

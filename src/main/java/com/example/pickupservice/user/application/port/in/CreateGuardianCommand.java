package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.image.ProfileImage;

public record CreateGuardianCommand(
		long vehicleId,
		String phone,
		String name,
		ProfileImage profileImage
) {

	public CreateGuardianCommand(long vehicleId, String phone, String name) {
		this(vehicleId, phone, name, null);
	}

	public CreateGuardianCommand withProfileImage(ProfileImage profileImage) {
		return new CreateGuardianCommand(vehicleId, phone, name, profileImage);
	}
}

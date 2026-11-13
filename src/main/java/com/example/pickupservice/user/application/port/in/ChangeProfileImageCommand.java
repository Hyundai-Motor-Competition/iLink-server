package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.image.ProfileImage;

public record ChangeProfileImageCommand(
		long userId,
		ProfileImage profileImage
) {
}

package com.example.pickupservice.common.web;

import com.example.pickupservice.common.image.ProfileImage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public final class ProfileImageParts {

	private ProfileImageParts() {
	}

	public static ProfileImage toProfileImage(MultipartFile part) throws IOException {
		if (part == null || part.isEmpty()) {
			return null;
		}
		return ProfileImage.of(part.getBytes());
	}
}

package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.domain.User;

import java.util.Optional;

final class ProfileImageUrls {

	private ProfileImageUrls() {
	}

	static String of(User user, ProfileImageStoragePort storage) {
		String key = user.getProfileImageKey();
		Optional<String> url = key != null ? storage.presignedUrl(key) : storage.defaultUrl();
		return url.orElse(null);
	}
}

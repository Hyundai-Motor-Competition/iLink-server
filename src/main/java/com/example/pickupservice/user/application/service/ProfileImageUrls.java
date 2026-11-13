package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.domain.User;

import java.util.Optional;

final class ProfileImageUrls {

	private ProfileImageUrls() {
	}

	static Optional<String> of(ProfileImageStoragePort storagePort, User user) {
		String key = user.getProfileImageKey();
		return key != null ? storagePort.presignedUrl(key) : storagePort.defaultUrl();
	}
}

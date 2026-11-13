package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;

final class ProfileImageUploads {

	private ProfileImageUploads() {
	}

	static String upload(ProfileImageStoragePort storagePort, ProfileImage image) {
		String key = image.newKey();
		storagePort.put(key, image.bytes(), image.format().contentType());
		return key;
	}
}

package com.example.pickupservice.user.application.port.out;

import java.util.Optional;

public interface ProfileImageStoragePort {

	void put(String key, byte[] bytes, String contentType);

	void delete(String key);

	Optional<String> presignedUrl(String key);

	Optional<String> defaultUrl();

	boolean configured();
}

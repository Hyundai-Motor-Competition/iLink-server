package com.example.pickupservice.user.fixture;

import com.example.pickupservice.common.image.StorageUnavailableException;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryProfileImageStorage implements ProfileImageStoragePort {

	public static final String DEFAULT_AVATAR_KEY = "defaults/avatar.png";
	private static final String URL_PREFIX = "https://storage.test/ilink/";
	private static final String URL_SUFFIX = "?sig=test";

	private final Map<String, StoredObject> objects = new LinkedHashMap<>();
	private final List<String> deleted = new ArrayList<>();
	private boolean failUploads;
	private boolean configured = true;

	@Override
	public synchronized void put(String key, byte[] bytes, String contentType) {
		if (failUploads || !configured) {
			throw new StorageUnavailableException();
		}
		objects.put(key, new StoredObject(bytes.clone(), contentType));
	}

	@Override
	public synchronized void delete(String key) {
		deleted.add(key);
		objects.remove(key);
	}

	@Override
	public synchronized Optional<String> presignedUrl(String key) {
		if (!configured) {
			return Optional.empty();
		}
		return Optional.of(URL_PREFIX + key + URL_SUFFIX);
	}

	@Override
	public Optional<String> defaultUrl() {
		return presignedUrl(DEFAULT_AVATAR_KEY);
	}

	@Override
	public synchronized boolean configured() {
		return configured;
	}

	public synchronized Optional<StoredObject> stored(String key) {
		return Optional.ofNullable(objects.get(key));
	}

	public synchronized List<String> storedKeys() {
		return List.copyOf(objects.keySet());
	}

	public synchronized List<String> deletedKeys() {
		return List.copyOf(deleted);
	}

	public synchronized void failUploads() {
		failUploads = true;
	}

	public synchronized void unconfigure() {
		configured = false;
	}

	public synchronized void reset() {
		objects.clear();
		deleted.clear();
		failUploads = false;
		configured = true;
	}

	public record StoredObject(byte[] bytes, String contentType) {
	}
}

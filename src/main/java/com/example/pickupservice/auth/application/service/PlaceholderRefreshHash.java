package com.example.pickupservice.auth.application.service;

import java.security.SecureRandom;
import java.util.HexFormat;

final class PlaceholderRefreshHash {

	private static final SecureRandom RANDOM = new SecureRandom();

	private PlaceholderRefreshHash() {
	}

	static String next() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}
}

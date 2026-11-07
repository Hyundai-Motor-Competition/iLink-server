package com.example.pickupservice.common.idempotency;

import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
final class RequestHasher {

	private static final String ALGORITHM = "SHA-256";

	static String hash(CachedBodyRequest request) {
		MessageDigest digest = sha256();
		digest.update((request.getMethod() + " " + request.getRequestURI() + "\n").getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(digest.digest(request.body()));
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance(ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}

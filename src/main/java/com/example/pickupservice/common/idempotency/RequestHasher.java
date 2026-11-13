package com.example.pickupservice.common.idempotency;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
final class RequestHasher {

	private static final String ALGORITHM = "SHA-256";
	private static final String REQUEST_PART = "request";
	private static final String PROFILE_IMAGE_PART = "profileImage";
	private static final String NO_IMAGE = "-";

	static String hash(CachedBodyRequest request) {
		MessageDigest digest = sha256();
		digest.update((request.getMethod() + " " + request.getRequestURI() + "\n").getBytes(StandardCharsets.UTF_8));
		if (request.multipart()) {
			updateWithParts(digest, request);
			return HexFormat.of().formatHex(digest.digest());
		}
		return HexFormat.of().formatHex(digest.digest(request.body()));
	}

	private static void updateWithParts(MessageDigest digest, CachedBodyRequest request) {
		try {
			digest.update(partBytes(request.getPart(REQUEST_PART)));
			digest.update("\n".getBytes(StandardCharsets.UTF_8));
			byte[] image = partBytes(request.getPart(PROFILE_IMAGE_PART));
			String imageHash = image.length == 0 ? NO_IMAGE : HexFormat.of().formatHex(sha256().digest(image));
			digest.update(imageHash.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (ServletException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[] partBytes(Part part) throws IOException {
		if (part == null) {
			return new byte[0];
		}
		try (InputStream input = part.getInputStream()) {
			return input.readAllBytes();
		}
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance(ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}

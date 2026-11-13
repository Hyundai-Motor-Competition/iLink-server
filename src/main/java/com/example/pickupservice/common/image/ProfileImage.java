package com.example.pickupservice.common.image;

import java.util.Arrays;
import java.util.UUID;

public record ProfileImage(byte[] bytes, ImageFormat format) {

	public static final int MAX_BYTES = 5_242_880;

	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
	private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
	private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

	public static ProfileImage of(byte[] bytes) {
		if (bytes.length > MAX_BYTES) {
			throw new ProfileImageTooLargeException("프로필 사진은 " + MAX_BYTES + "바이트를 넘을 수 없다");
		}
		return new ProfileImage(bytes, detect(bytes));
	}

	public String newKey() {
		return "profiles/" + UUID.randomUUID() + "." + format.extension();
	}

	private static ImageFormat detect(byte[] bytes) {
		if (matchesAt(bytes, 0, JPEG_MAGIC)) {
			return ImageFormat.JPEG;
		}
		if (matchesAt(bytes, 0, PNG_MAGIC)) {
			return ImageFormat.PNG;
		}
		if (matchesAt(bytes, 0, RIFF) && matchesAt(bytes, 8, WEBP)) {
			return ImageFormat.WEBP;
		}
		throw new ProfileImageInvalidException("프로필 사진은 JPEG · PNG · WEBP만 받는다");
	}

	private static boolean matchesAt(byte[] bytes, int offset, byte[] magic) {
		int end = offset + magic.length;
		return bytes.length >= end && Arrays.equals(bytes, offset, end, magic, 0, magic.length);
	}
}

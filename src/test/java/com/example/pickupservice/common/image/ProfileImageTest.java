package com.example.pickupservice.common.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageTest {

	private static final int MAX_BYTES = 5_242_880;

	private static final byte[] JPEG_MAGIC = bytes(0xFF, 0xD8, 0xFF);
	private static final byte[] PNG_MAGIC = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);

	private static byte[] bytes(int... values) {
		byte[] result = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = (byte) values[i];
		}
		return result;
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	private static byte[] concat(byte[]... parts) {
		int length = 0;
		for (byte[] part : parts) {
			length += part.length;
		}
		byte[] result = new byte[length];
		int offset = 0;
		for (byte[] part : parts) {
			System.arraycopy(part, 0, result, offset, part.length);
			offset += part.length;
		}
		return result;
	}

	private static byte[] withSize(byte[] magic, int size) {
		byte[] result = new byte[size];
		System.arraycopy(magic, 0, result, 0, magic.length);
		return result;
	}

	private static byte[] jpeg() {
		return concat(JPEG_MAGIC, bytes(0xE0, 0x00, 0x10), ascii("JFIF"));
	}

	private static byte[] png() {
		return concat(PNG_MAGIC, bytes(0x00, 0x00, 0x00, 0x0D), ascii("IHDR"));
	}

	private static byte[] webp() {
		return concat(ascii("RIFF"), bytes(0x24, 0x00, 0x00, 0x00), ascii("WEBP"), ascii("VP8 "));
	}

	static Stream<Arguments> supportedImages() {
		return Stream.of(
				Arguments.of("JPEG", jpeg(), ImageFormat.JPEG, "jpg", "image/jpeg"),
				Arguments.of("PNG", png(), ImageFormat.PNG, "png", "image/png"),
				Arguments.of("WEBP", webp(), ImageFormat.WEBP, "webp", "image/webp")
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("supportedImages")
	@DisplayName("ProfileImage.of는 JPEG · PNG · WEBP 매직 넘버를 판정해 형식과 확장자 · Content-Type을 정한다")
	void detectsFormatFromMagicNumber(String label, byte[] data, ImageFormat expectedFormat,
			String expectedExtension, String expectedContentType) {
		ProfileImage image = ProfileImage.of(data);

		assertThat(image.format()).isEqualTo(expectedFormat);
		assertThat(image.format().extension()).isEqualTo(expectedExtension);
		assertThat(image.format().contentType()).isEqualTo(expectedContentType);
		assertThat(image.bytes()).isEqualTo(data);
	}

	@Test
	@DisplayName("ProfileImage.of는 매직 넘버만 있는 최소 길이 파일도 형식을 판정한다")
	void detectsFormatFromMagicNumberOnly() {
		assertThat(ProfileImage.of(JPEG_MAGIC.clone()).format()).isEqualTo(ImageFormat.JPEG);
		assertThat(ProfileImage.of(PNG_MAGIC.clone()).format()).isEqualTo(ImageFormat.PNG);
		assertThat(ProfileImage.of(concat(ascii("RIFF"), bytes(0, 0, 0, 0), ascii("WEBP"))).format())
				.isEqualTo(ImageFormat.WEBP);
	}

	static Stream<Arguments> invalidImages() {
		return Stream.of(
				Arguments.of("GIF", ascii("GIF89a\u0000\u0000")),
				Arguments.of("텍스트", ascii("hello world, not an image")),
				Arguments.of("빈 배열", new byte[0]),
				Arguments.of("JPEG 앞 2바이트만", bytes(0xFF, 0xD8)),
				Arguments.of("JPEG 셋째 바이트 틀림", bytes(0xFF, 0xD8, 0x00, 0x00)),
				Arguments.of("PNG 마지막 바이트 틀림", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x00)),
				Arguments.of("PNG 앞 7바이트만", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A)),
				Arguments.of("RIFF WAVE", concat(ascii("RIFF"), bytes(0x24, 0, 0, 0), ascii("WAVE"))),
				Arguments.of("RIFF만 있고 짧음", concat(ascii("RIFF"), bytes(0x24, 0, 0, 0), ascii("WEB"))),
				Arguments.of("WEBP가 0번 위치", concat(ascii("WEBP"), bytes(0x24, 0, 0, 0), ascii("RIFF")))
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidImages")
	@DisplayName("ProfileImage.of는 매직 넘버가 JPEG · PNG · WEBP가 아니면 ProfileImageInvalidException을 던진다")
	void rejectsUnsupportedMagicNumber(String label, byte[] data) {
		assertThatThrownBy(() -> ProfileImage.of(data))
				.isInstanceOf(ProfileImageInvalidException.class);
	}

	@Test
	@DisplayName("ProfileImage.of는 딱 5,242,880바이트는 받는다")
	void acceptsExactlyMaxBytes() {
		byte[] data = withSize(JPEG_MAGIC, MAX_BYTES);

		ProfileImage image = ProfileImage.of(data);

		assertThat(image.format()).isEqualTo(ImageFormat.JPEG);
		assertThat(image.bytes()).hasSize(MAX_BYTES);
	}

	@Test
	@DisplayName("ProfileImage.of는 5,242,880바이트를 넘으면 ProfileImageTooLargeException을 던진다")
	void rejectsOverMaxBytes() {
		byte[] data = withSize(PNG_MAGIC, MAX_BYTES + 1);

		assertThatThrownBy(() -> ProfileImage.of(data))
				.isInstanceOf(ProfileImageTooLargeException.class);
	}

	@Test
	@DisplayName("ProfileImage.of는 5,242,880바이트를 넘으면 형식이 틀려도 ProfileImageTooLargeException을 던진다")
	void rejectsOverMaxBytesBeforeFormatCheck() {
		byte[] data = withSize(ascii("GIF89a"), MAX_BYTES + 1);

		assertThatThrownBy(() -> ProfileImage.of(data))
				.isInstanceOf(ProfileImageTooLargeException.class);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("supportedImages")
	@DisplayName("ProfileImage.newKey는 profiles/{UUID}.{ext} 형식의 키를 만든다")
	void newKeyHasProfilesUuidExtensionFormat(String label, byte[] data, ImageFormat expectedFormat,
			String expectedExtension, String expectedContentType) {
		String key = ProfileImage.of(data).newKey();

		assertThat(key).startsWith("profiles/").endsWith("." + expectedExtension);
		String uuidPart = key.substring("profiles/".length(), key.length() - expectedExtension.length() - 1);
		assertThat(UUID.fromString(uuidPart).toString()).isEqualTo(uuidPart);
	}

	@Test
	@DisplayName("ProfileImage.newKey는 부를 때마다 다른 키를 만든다")
	void newKeyIsUniqueEachCall() {
		ProfileImage image = ProfileImage.of(jpeg());

		assertThat(image.newKey()).isNotEqualTo(image.newKey());
	}
}

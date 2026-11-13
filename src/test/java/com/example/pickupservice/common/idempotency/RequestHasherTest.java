package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.fixture.MultipartBodies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockPart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class RequestHasherTest {

	private static final String PATH = "/v1/families/me/members";
	private static final String REQUEST_JSON = "{\"role\":\"CHILD\",\"name\":\"하준\",\"phone\":null}";
	private static final byte[] PNG_A = {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52};
	private static final byte[] PNG_B = {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x53};

	@Test
	@DisplayName("multipart 요청의 Idempotency 해시는 경계 문자열이 달라도 request 파트와 사진 바이트가 같으면 같고 값은 SHA-256(메서드 경로 \\n request 파트 \\n 사진 SHA-256 16진수)이다")
	void multipartHashIgnoresBoundaryWhenRequestPartAndImageAreSame() throws IOException {
		String first = RequestHasher.hash(new CachedBodyRequest(multipartRequest("boundary-AAAA1111", PNG_A)));
		String second = RequestHasher.hash(new CachedBodyRequest(multipartRequest("boundary-BBBB2222", PNG_A)));

		assertThat(second).isEqualTo(first);
		assertThat(first).isEqualTo(expectedMultipartHash(PNG_A));
	}

	@Test
	@DisplayName("multipart 요청의 Idempotency 해시는 경계 문자열이 같아도 사진 바이트가 다르면 달라지고 각각 SHA-256(메서드 경로 \\n request 파트 \\n 사진 SHA-256 16진수)이다")
	void multipartHashChangesWhenImageBytesDiffer() throws IOException {
		String withA = RequestHasher.hash(new CachedBodyRequest(multipartRequest("boundary-SAME", PNG_A)));
		String withB = RequestHasher.hash(new CachedBodyRequest(multipartRequest("boundary-SAME", PNG_B)));

		assertThat(withB).isNotEqualTo(withA);
		assertThat(withA).isEqualTo(expectedMultipartHash(PNG_A));
		assertThat(withB).isEqualTo(expectedMultipartHash(PNG_B));
	}

	@Test
	@DisplayName("multipart 요청의 Idempotency 해시는 profileImage 파트가 없거나 0바이트면 사진 자리에 \"-\"를 넣어 SHA-256(메서드 경로 \\n request 파트 \\n -)으로 계산한다")
	void multipartHashUsesDashWhenImagePartIsMissingOrEmpty() throws IOException {
		String expected = expectedMultipartHashWithoutImage();

		String missing = RequestHasher.hash(new CachedBodyRequest(multipartRequest("boundary-NOIMG", null)));
		String empty = RequestHasher.hash(new CachedBodyRequest(multipartRequest("boundary-EMPTY", new byte[0])));

		assertThat(missing).isEqualTo(expected);
		assertThat(empty).isEqualTo(expected);
		assertThat(empty).isEqualTo(missing);
	}

	private static MockHttpServletRequest multipartRequest(String boundary, byte[] image) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);
		request.setContentType(MultipartBodies.contentType(boundary));
		request.setContent(MultipartBodies.requestWithImage(boundary, REQUEST_JSON, image));
		MockPart requestPart = new MockPart("request", REQUEST_JSON.getBytes(StandardCharsets.UTF_8));
		requestPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		request.addPart(requestPart);
		if (image == null) {
			return request;
		}
		MockPart imagePart = new MockPart("profileImage", "photo.png", image);
		imagePart.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE);
		request.addPart(imagePart);
		return request;
	}

	private static String expectedMultipartHash(byte[] image) {
		ByteArrayOutputStream input = new ByteArrayOutputStream();
		input.writeBytes(("POST " + PATH + "\n").getBytes(StandardCharsets.UTF_8));
		input.writeBytes(REQUEST_JSON.getBytes(StandardCharsets.UTF_8));
		input.writeBytes("\n".getBytes(StandardCharsets.UTF_8));
		input.writeBytes(HexFormat.of().formatHex(sha256(image)).getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(sha256(input.toByteArray()));
	}

	private static String expectedMultipartHashWithoutImage() {
		ByteArrayOutputStream input = new ByteArrayOutputStream();
		input.writeBytes(("POST " + PATH + "\n").getBytes(StandardCharsets.UTF_8));
		input.writeBytes(REQUEST_JSON.getBytes(StandardCharsets.UTF_8));
		input.writeBytes("\n".getBytes(StandardCharsets.UTF_8));
		input.writeBytes("-".getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(sha256(input.toByteArray()));
	}

	private static byte[] sha256(byte[] bytes) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}

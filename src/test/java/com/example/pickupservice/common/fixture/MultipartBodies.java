package com.example.pickupservice.common.fixture;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class MultipartBodies {

	private static final String CRLF = "\r\n";

	private MultipartBodies() {
	}

	public static String contentType(String boundary) {
		return "multipart/form-data; boundary=" + boundary;
	}

	public static byte[] requestWithImage(String boundary, String requestJson, byte[] image) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		write(out, "--" + boundary + CRLF);
		write(out, "Content-Disposition: form-data; name=\"request\"" + CRLF);
		write(out, "Content-Type: application/json" + CRLF + CRLF);
		write(out, requestJson);
		write(out, CRLF);
		if (image != null) {
			write(out, "--" + boundary + CRLF);
			write(out, "Content-Disposition: form-data; name=\"profileImage\"; filename=\"photo.png\"" + CRLF);
			write(out, "Content-Type: image/png" + CRLF + CRLF);
			out.writeBytes(image);
			write(out, CRLF);
		}
		write(out, "--" + boundary + "--" + CRLF);
		return out.toByteArray();
	}

	private static void write(ByteArrayOutputStream out, String text) {
		out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
	}
}

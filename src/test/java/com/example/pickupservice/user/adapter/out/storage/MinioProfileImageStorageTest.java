package com.example.pickupservice.user.adapter.out.storage;

import com.example.pickupservice.common.image.StorageUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinioProfileImageStorageTest {

	private static final String INTERNAL_ENDPOINT = "http://minio.internal.invalid:9000";
	private static final String PUBLIC_ENDPOINT = "https://img-dev.chisanw.com";
	private static final String BUCKET = "ilink";

	private static ProfileImageProperties properties(String endpoint, Duration urlTtl) {
		return new ProfileImageProperties(endpoint, PUBLIC_ENDPOINT, BUCKET, "test-access-key", "test-secret-key", urlTtl);
	}

	private static Map<String, String> query(URI uri) {
		Map<String, String> result = new HashMap<>();
		for (String pair : uri.getRawQuery().split("&")) {
			int eq = pair.indexOf('=');
			result.put(pair.substring(0, eq), pair.substring(eq + 1));
		}
		return result;
	}

	@ParameterizedTest(name = "url-ttl {0} -> X-Amz-Expires {1}")
	@CsvSource({"PT10M, 600", "PT5M, 300"})
	@DisplayName("presignedUrl은 MINIO_PUBLIC_ENDPOINT 주소의 /{bucket}/{key} 경로로 X-Amz-Expires가 url-ttl 초인 서명 URL을 네트워크 없이 만든다")
	void presignedUrlUsesPublicEndpointAndUrlTtlSeconds(String ttl, String expectedSeconds) {
		MinioProfileImageStorage storage = new MinioProfileImageStorage(properties(INTERNAL_ENDPOINT, Duration.parse(ttl)));

		Optional<String> url = storage.presignedUrl("profiles/abc.png");

		assertThat(storage.configured()).isTrue();
		assertThat(url).isPresent();
		URI uri = URI.create(url.get());
		assertThat(uri.getScheme()).isEqualTo("https");
		assertThat(uri.getHost()).isEqualTo("img-dev.chisanw.com");
		assertThat(uri.getPath()).isEqualTo("/ilink/profiles/abc.png");
		Map<String, String> query = query(uri);
		assertThat(query).containsEntry("X-Amz-Expires", expectedSeconds);
		assertThat(query).containsEntry("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
		assertThat(query.get("X-Amz-Signature")).isNotBlank();
	}

	@Test
	@DisplayName("defaultUrl은 MINIO_PUBLIC_ENDPOINT 주소의 defaults/avatar.png 서명 URL이다")
	void defaultUrlIsPresignedUrlOfDefaultAvatar() {
		MinioProfileImageStorage storage = new MinioProfileImageStorage(properties(INTERNAL_ENDPOINT, Duration.ofMinutes(10)));

		Optional<String> url = storage.defaultUrl();

		assertThat(url).isPresent();
		URI uri = URI.create(url.get());
		assertThat(uri.getHost()).isEqualTo("img-dev.chisanw.com");
		assertThat(uri.getPath()).isEqualTo("/ilink/defaults/avatar.png");
		assertThat(query(uri)).containsEntry("X-Amz-Expires", "600");
	}

	@ParameterizedTest(name = "endpoint=[{0}]")
	@NullAndEmptySource
	@DisplayName("MINIO_ENDPOINT가 비면 configured()가 false이고 presignedUrl · defaultUrl이 비어 있고 put은 StorageUnavailableException이다")
	void notConfiguredWhenEndpointIsEmpty(String endpoint) {
		MinioProfileImageStorage storage = new MinioProfileImageStorage(properties(endpoint, Duration.ofMinutes(10)));

		assertThat(storage.configured()).isFalse();
		assertThat(storage.presignedUrl("profiles/abc.png")).isEmpty();
		assertThat(storage.defaultUrl()).isEmpty();
		assertThatThrownBy(() -> storage.put("profiles/abc.png", new byte[]{1, 2, 3}, "image/png"))
				.isInstanceOf(StorageUnavailableException.class);
	}
}

package com.example.pickupservice.common.error;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.InMemoryProfileImageStorageConfiguration;
import com.example.pickupservice.common.fixture.TestPostgisConfiguration;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.RegisterMemberUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.TestImages.PNG_SIGNATURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import({TestPostgisConfiguration.class, AccessTokenFixture.class, InMemoryProfileImageStorageConfiguration.class})
class GlobalExceptionHandlerRealServerTest {

	private static final String MEMBERS_PATH = "/v1/families/me/members";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CHILD_USER_ID = 131L;
	private static final int MB = 1024 * 1024;
	private static final String CHILD_MEMBER_JSON = """
			{"role":"CHILD","name":"민준","phone":null}
			""";

	private final RestClient restClient;
	private final AccessTokenFixture accessTokenFixture;

	@MockitoBean
	private RegisterMemberUseCase registerMemberUseCase;

	GlobalExceptionHandlerRealServerTest(@LocalServerPort int port, AccessTokenFixture accessTokenFixture) {
		this.restClient = RestClient.create("http://localhost:" + port);
		this.accessTokenFixture = accessTokenFixture;
	}

	@Test
	@DisplayName("실제 서버에서 POST /v1/families/me/members에 7MB 사진을 multipart로 보내면 컨트롤러 전에 거절돼도 413과 공통 오류 본문 error.code PROFILE_IMAGE_TOO_LARGE를 돌려주고 유스케이스를 부르지 않는다")
	void multipartOverFileLimitOnRealServerReturns413ProfileImageTooLarge() {
		ResponseEntity<Map<String, Object>> response = postMultipart(parts(png(7 * MB)));

		assertProfileImageTooLarge(response);
		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("실제 서버에서 사진은 5MB 이하지만 요청 전체가 6MB를 넘는 multipart를 보내면 413과 공통 오류 본문 error.code PROFILE_IMAGE_TOO_LARGE를 돌려주고 유스케이스를 부르지 않는다")
	void multipartOverRequestLimitOnRealServerReturns413ProfileImageTooLarge() {
		MultiValueMap<String, Object> parts = parts(png(4 * MB));
		parts.add("padding", fileEntity(filled(3 * MB), "padding.bin", MediaType.APPLICATION_OCTET_STREAM));

		ResponseEntity<Map<String, Object>> response = postMultipart(parts);

		assertProfileImageTooLarge(response);
		then(registerMemberUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("실제 서버에서 4MB 사진(5MB 이하, 요청 전체 6MB 이하)을 multipart로 보내면 한도에 걸리지 않고 201과 userId를 돌려준다")
	void multipartWithin5MbImageOnRealServerReturns201() {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		ResponseEntity<Map<String, Object>> response = postMultipart(parts(png(4 * MB)));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).containsEntry("userId", (int) CHILD_USER_ID);
	}

	@Test
	@DisplayName("실제 서버에서 16KB 사진을 multipart로 보내면 201과 userId를 돌려준다")
	void multipartWithSmallImageOnRealServerReturns201() {
		given(registerMemberUseCase.registerMember(any())).willReturn(CHILD_USER_ID);

		ResponseEntity<Map<String, Object>> response = postMultipart(parts(png(16 * 1024)));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).containsEntry("userId", (int) CHILD_USER_ID);
	}

	@SuppressWarnings("unchecked")
	private static void assertProfileImageTooLarge(ResponseEntity<Map<String, Object>> response) {
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
		assertThat(response.getHeaders().getContentType()).isNotNull();
		assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
		assertThat(response.getBody()).containsKey("error");
		Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
		assertThat(error).containsEntry("code", "PROFILE_IMAGE_TOO_LARGE");
		assertThat(error).containsKeys("message", "retryAfterSeconds");
		assertThat(error.get("message")).asString().isNotBlank();
		assertThat(error.get("retryAfterSeconds")).isNull();
		assertThat(error).doesNotContainKey("fields");
	}

	private ResponseEntity<Map<String, Object>> postMultipart(MultiValueMap<String, Object> parts) {
		return restClient.post()
				.uri(MEMBERS_PATH)
				.header(HttpHeaders.AUTHORIZATION,
						bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN)))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(parts)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});
	}

	private static MultiValueMap<String, Object> parts(byte[] image) {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);
		parts.add("request", new HttpEntity<>(CHILD_MEMBER_JSON.getBytes(StandardCharsets.UTF_8), requestHeaders));
		parts.add("profileImage", fileEntity(image, "child.png", MediaType.IMAGE_PNG));
		return parts;
	}

	private static HttpEntity<ByteArrayResource> fileEntity(byte[] bytes, String filename, MediaType contentType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(contentType);
		ByteArrayResource resource = new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		};
		return new HttpEntity<>(resource, headers);
	}

	private static byte[] png(int size) {
		byte[] bytes = filled(size);
		System.arraycopy(PNG_SIGNATURE, 0, bytes, 0, PNG_SIGNATURE.length);
		return bytes;
	}

	private static byte[] filled(int size) {
		byte[] bytes = new byte[size];
		Arrays.fill(bytes, (byte) 0x41);
		return bytes;
	}

}

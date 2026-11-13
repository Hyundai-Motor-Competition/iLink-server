package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.InMemoryProfileImageStorageConfiguration;
import com.example.pickupservice.common.fixture.MultipartBodies;
import com.example.pickupservice.common.fixture.TestPostgisConfiguration;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage;
import com.example.pickupservice.user.fixture.UserSeed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.TestImages.PNG_IHDR_BYTES;
import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import({TestPostgisConfiguration.class, AccessTokenFixture.class, InMemoryProfileImageStorageConfiguration.class,
		UserSeed.class})
class FamilyControllerRealServerTest {

	private static final String MEMBERS_PATH = "/v1/families/me/members";
	private static final String NEW_CHILD_NAME = "하준";
	private static final String REQUEST_JSON = "{\"role\":\"CHILD\",\"name\":\"" + NEW_CHILD_NAME + "\",\"phone\":null}";
	private static final String BOUNDARY = "real-server-boundary-7f3a";
	private final RestClient restClient;
	private final AccessTokenFixture accessTokenFixture;
	private final UserSeed userSeed;
	private final InMemoryProfileImageStorage storage;
	private final JdbcTemplate jdbcTemplate;

	private UserSeed.Result seeded;

	FamilyControllerRealServerTest(@LocalServerPort int port, AccessTokenFixture accessTokenFixture, UserSeed userSeed,
			InMemoryProfileImageStorage storage, JdbcTemplate jdbcTemplate) {
		this.restClient = RestClient.create("http://localhost:" + port);
		this.accessTokenFixture = accessTokenFixture;
		this.userSeed = userSeed;
		this.storage = storage;
		this.jdbcTemplate = jdbcTemplate;
	}

	@BeforeEach
	void setUp() {
		storage.reset();
		seeded = userSeed.seed();
	}

	@AfterEach
	void cleanUp() {
		userSeed.clear(seeded);
		storage.reset();
	}

	@Test
	@DisplayName("실제 서블릿 컨테이너(RANDOM_PORT)에서 Idempotency 필터를 거친 POST /v1/families/me/members multipart 요청이 request · profileImage 파트를 읽어 201과 userId를 돌려주고 사진을 저장소에 올린다")
	void multipartRegisterMemberThroughIdempotencyFilterOnRealServerReturns201() {
		ResponseEntity<Map<String, Object>> response = restClient.post()
				.uri(MEMBERS_PATH)
				.header(HttpHeaders.AUTHORIZATION,
						bearer(accessTokenFixture.user(seeded.guardianId(), seeded.familyId(), Role.GUARDIAN)))
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.parseMediaType(MultipartBodies.contentType(BOUNDARY)))
				.body(MultipartBodies.requestWithImage(BOUNDARY, REQUEST_JSON, PNG_IHDR_BYTES))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).containsKey("userId");
		long userId = ((Number) response.getBody().get("userId")).longValue();
		String storedKey = jdbcTemplate.queryForObject(
				"select profile_image_key from users where user_id = ? and family_id = ? and name = ?",
				String.class, userId, seeded.familyId(), NEW_CHILD_NAME);
		assertThat(storage.storedKeys()).containsExactly(storedKey);
		assertThat(storage.stored(storedKey)).hasValueSatisfying(
				stored -> assertThat(stored.bytes()).isEqualTo(PNG_IHDR_BYTES));
	}
}

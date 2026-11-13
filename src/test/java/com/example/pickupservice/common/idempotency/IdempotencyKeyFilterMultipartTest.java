package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.fixture.MultipartBodies;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.TestImages.PNG_IHDR_BYTES;
import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@RequiredArgsConstructor
@Import(UserSeed.class)
class IdempotencyKeyFilterMultipartTest extends IntegrationTestSupport {

	private static final String MEMBERS_PATH = "/v1/families/me/members";
	private static final String NEW_CHILD_NAME = "하준";
	private static final String REQUEST_JSON = "{\"role\":\"CHILD\",\"name\":\"" + NEW_CHILD_NAME + "\",\"phone\":null}";
	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;
	private final UserSeed userSeed;
	private final InMemoryProfileImageStorage storage;
	private final JdbcTemplate jdbcTemplate;

	private UserSeed.Result seeded;

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
	@DisplayName("같은 Idempotency-Key로 경계만 다른 multipart 멤버 등록을 다시 보내면 두 번째도 처음과 같은 201 본문을 받고 사진은 저장소에 한 번만 올라가며 멤버도 한 명만 생긴다")
	void multipartRetryWithDifferentBoundaryReplaysFirstResponseWithoutReuploading() throws Exception {
		String key = UUID.randomUUID().toString();

		MvcResult first = mockMvc.perform(registerMember(key, "boundary-first-AAAA")).andReturn();
		MvcResult second = mockMvc.perform(registerMember(key, "boundary-retry-BBBB")).andReturn();

		assertThat(first.getResponse().getStatus()).isEqualTo(201);
		assertThat(second.getResponse().getStatus()).isEqualTo(201);
		JSONAssert.assertEquals(first.getResponse().getContentAsString(StandardCharsets.UTF_8),
				second.getResponse().getContentAsString(StandardCharsets.UTF_8), JSONCompareMode.STRICT);
		assertThat(storage.storedKeys()).hasSize(1);
		Integer created = jdbcTemplate.queryForObject(
				"select count(*) from users where family_id = ? and name = ?",
				Integer.class, seeded.familyId(), NEW_CHILD_NAME);
		assertThat(created).isEqualTo(1);
	}

	private MockMultipartHttpServletRequestBuilder registerMember(String idempotencyKey, String boundary) {
		MockPart requestPart = new MockPart("request", REQUEST_JSON.getBytes(StandardCharsets.UTF_8));
		requestPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		MockPart imagePart = new MockPart("profileImage", "photo.png", PNG_IHDR_BYTES);
		imagePart.getHeaders().setContentType(MediaType.IMAGE_PNG);
		return multipart(MEMBERS_PATH)
				.part(requestPart, imagePart)
				.contentType(MultipartBodies.contentType(boundary))
				.content(MultipartBodies.requestWithImage(boundary, REQUEST_JSON, PNG_IHDR_BYTES))
				.header(HttpHeaders.AUTHORIZATION,
						bearer(accessTokenFixture.user(seeded.guardianId(), seeded.familyId(), Role.GUARDIAN)))
				.header(IdempotencyKeyHeader.NAME, idempotencyKey);
	}
}

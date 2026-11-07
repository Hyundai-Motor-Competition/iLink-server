package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.OrderRequest;
import com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.IDEMPOTENCY_KEY_HEADER;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.OTHER_REQUEST_BODY;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.REQUEST_BODY;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.VALIDATION_FAILED_MESSAGE;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.bodyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class AnonymousIdempotencyKeyFilterTest extends IntegrationTestSupport {

	private static final String PUBLIC_TEST_ENDPOINT_PATTERN = "/test/idempotency-public/**";
	private static final String PUBLIC_IDEMPOTENT_ORDER_PATH = "/test/idempotency-public/orders";
	private static final String REPLAYED_KEY = "2c7e9a14-5b3d-4e8f-a6c1-9d0b2e4f6a83";
	private static final String REUSED_WITH_OTHER_BODY_KEY = "8e1f3a5c-7d9b-4c2e-b4a6-1f3d5b7e9c02";
	private static final String STORED_ONCE_KEY = "4b6d8f0a-2c4e-4a6b-9d8f-3e5a7c9b1d64";
	private static final String DIRECT_INSERT_KEY = "6f8a0c2e-4b6d-4e8a-8c0e-5d7f9b1a3c95";
	private static final String REQUEST_HASH = "a".repeat(64);

	private final MockMvc mockMvc;
	private final JdbcTemplate jdbcTemplate;
	private final PublicIdempotentOrderTestController publicIdempotentOrderTestController;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("delete from idempotency_key where idempotency_key in (?, ?, ?, ?)",
				REPLAYED_KEY, REUSED_WITH_OTHER_BODY_KEY, STORED_ONCE_KEY, DIRECT_INSERT_KEY);
	}

	@Test
	@DisplayName("인증 없이 부르는 Idempotency-Key 대상 POST API에 토큰 없이 같은 키 · 같은 본문을 두 번 보내면 두 번째 응답은 첫 응답과 같은 상태 201 · 같은 JSON 본문(호출마다 늘어나는 orderNumber까지 같음)이고 컨트롤러 메서드는 한 번만 실행된다")
	void anonymousSecondRequestWithSameKeyAndSameBodyReturnsFirstStatusAndBodyAndRunsUseCaseOnce() throws Exception {
		int callsBefore = publicIdempotentOrderTestController.createCallCount();

		MvcResult first = mockMvc.perform(anonymousIdempotentOrder(REPLAYED_KEY, REQUEST_BODY))
				.andExpect(status().isCreated())
				.andReturn();
		MvcResult second = mockMvc.perform(anonymousIdempotentOrder(REPLAYED_KEY, REQUEST_BODY))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();

		JSONAssert.assertEquals(bodyOf(first), bodyOf(second), JSONCompareMode.STRICT);
		assertThat(publicIdempotentOrderTestController.createCallCount() - callsBefore).isEqualTo(1);
	}

	@Test
	@DisplayName("인증 없이 부르는 Idempotency-Key 대상 POST API에 토큰 없이 키 K · 본문 A로 201을 받은 뒤 같은 키 K · 다른 본문 B를 보내면 409 IDEMPOTENCY_KEY_REUSED 공통 오류를 받고 컨트롤러 메서드는 첫 요청 때 한 번만 실행된다")
	void anonymousSameKeyWithOtherBodyGets409AndUseCaseRunsOnce() throws Exception {
		int callsBefore = publicIdempotentOrderTestController.createCallCount();

		mockMvc.perform(anonymousIdempotentOrder(REUSED_WITH_OTHER_BODY_KEY, REQUEST_BODY))
				.andExpect(status().isCreated());
		mockMvc.perform(anonymousIdempotentOrder(REUSED_WITH_OTHER_BODY_KEY, OTHER_REQUEST_BODY))
				.andExpect(commonError(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED"));

		assertThat(publicIdempotentOrderTestController.createCallCount() - callsBefore).isEqualTo(1);
	}

	@Test
	@DisplayName("인증 없이 부르는 Idempotency-Key 대상 POST API에 토큰 없이 같은 키 · 같은 본문을 두 번 보내면 idempotency_key에 그 키의 행이 principal_type null · principal_id null · response_status 201로 정확히 1개만 남는다")
	void anonymousSameKeyTwiceStoresExactlyOneRowWithNullPrincipal() throws Exception {
		mockMvc.perform(anonymousIdempotentOrder(STORED_ONCE_KEY, REQUEST_BODY))
				.andExpect(status().isCreated());
		mockMvc.perform(anonymousIdempotentOrder(STORED_ONCE_KEY, REQUEST_BODY))
				.andExpect(status().isCreated());

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"select principal_type, principal_id, response_status from idempotency_key where idempotency_key = ?",
				STORED_ONCE_KEY);
		assertThat(rows).hasSize(1);
		assertThat(rows.getFirst().get("principal_type")).isNull();
		assertThat(rows.getFirst().get("principal_id")).isNull();
		assertThat(rows.getFirst().get("response_status")).isEqualTo(HttpStatus.CREATED.value());
	}

	@Test
	@DisplayName("인증 없이 부르는 Idempotency-Key 대상 POST API를 토큰 없이 Idempotency-Key 헤더도 없이 부르면 422 VALIDATION_FAILED와 fields[0].field Idempotency-Key를 받고 컨트롤러 메서드는 실행되지 않는다")
	void anonymousRequestWithoutIdempotencyKeyHeaderGets422AndUseCaseDoesNotRun() throws Exception {
		int callsBefore = publicIdempotentOrderTestController.createCallCount();

		mockMvc.perform(post(PUBLIC_IDEMPOTENT_ORDER_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY.getBytes(StandardCharsets.UTF_8)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.message").value(VALIDATION_FAILED_MESSAGE))
				.andExpect(jsonPath("$.error.fields", hasSize(1)))
				.andExpect(jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER));

		assertThat(publicIdempotentOrderTestController.createCallCount() - callsBefore).isZero();
	}

	@Test
	@DisplayName("idempotency_key에 principal_type · principal_id가 null이고 키가 같은 행을 두 번 넣으면 두 번째는 유니크 위반으로 거절된다(NULLS NOT DISTINCT 유니크 인덱스)")
	void secondRowWithSameKeyAndNullPrincipalViolatesUniqueIndex() {
		insertAnonymousKeyRow(DIRECT_INSERT_KEY);

		assertThatThrownBy(() -> insertAnonymousKeyRow(DIRECT_INSERT_KEY))
				.isInstanceOf(DuplicateKeyException.class);
	}

	private void insertAnonymousKeyRow(String idempotencyKey) {
		jdbcTemplate.update("""
				insert into idempotency_key (principal_type, principal_id, idempotency_key, request_hash, created_at, updated_at)
				values (null, null, ?, ?, now(), now())""", idempotencyKey, REQUEST_HASH);
	}

	private static MockHttpServletRequestBuilder anonymousIdempotentOrder(String idempotencyKey, String requestBody) {
		return post(PUBLIC_IDEMPOTENT_ORDER_PATH)
				.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody.getBytes(StandardCharsets.UTF_8));
	}

	@RestController
	static class PublicIdempotentOrderTestController {

		private final AtomicLong orderNumbers = new AtomicLong();
		private final AtomicInteger createCalls = new AtomicInteger();

		@IdempotencyKeyRequired
		@PostMapping(PUBLIC_IDEMPOTENT_ORDER_PATH)
		public ResponseEntity<OrderResponse> create(@RequestBody OrderRequest request) {
			createCalls.incrementAndGet();
			OrderResponse response = OrderResponse.of(orderNumbers.incrementAndGet(), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		}

		public int createCallCount() {
			return createCalls.get();
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(PublicIdempotentOrderTestController.class)
	static class PublicIdempotentOrderTestEndpointConfiguration {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain publicIdempotentOrderSecurityFilterChain(HttpSecurity http) throws Exception {
			http.securityMatcher(PUBLIC_TEST_ENDPOINT_PATTERN)
					.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
					.csrf(AbstractHttpConfigurer::disable);
			return http.build();
		}

	}

}

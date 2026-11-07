package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.OrderRequest;
import com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.OrderResponse;
import com.example.pickupservice.common.security.Role;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.IDEMPOTENCY_KEY_HEADER;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.OTHER_REQUEST_BODY;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.REQUEST_BODY;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.VALIDATION_FAILED_MESSAGE;
import static com.example.pickupservice.common.idempotency.IdempotentOrderTestFixtures.bodyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class IdempotencyKeyFilterTest extends IntegrationTestSupport {

	private static final String IDEMPOTENT_ORDER_PATH = "/test/idempotency/orders";
	private static final String HELD_IDEMPOTENT_ORDER_PATH = "/test/idempotency/held-orders";
	private static final String FAILING_ONCE_IDEMPOTENT_ORDER_PATH = "/test/idempotency/failing-once-orders";
	private static final String NON_TARGET_ORDER_PATH = "/test/idempotency/non-target-orders";
	private static final String KEY = "c4a1e9d2-6b3f-4f7a-9e2d-5a8b1c0d3e47";
	private static final String OTHER_KEY = "e7b2f0a3-8c4d-4a6e-b1f9-2d3c4e5f6a78";
	private static final String CONCURRENT_KEY = "3d8f1a6c-2e4b-4c9d-a7f0-6b5e4d3c2a19";
	private static final String REUSED_WITH_OTHER_BODY_KEY = "9a2c4e6f-1b3d-4f5a-8c7e-0d2f4a6b8c91";
	private static final String REUSED_ON_OTHER_PATH_KEY = "5f7a9c1e-3d5b-4e7f-9a1c-2b4d6f8a0c35";
	private static final String NON_TARGET_KEY = "a6d4c2b0-9e8f-4d7c-b6a5-1f2e3d4c5b6a";
	private static final String FAILED_WITH_4XX_KEY = "b1c3e5a7-0f2d-4b6e-8a9c-7d5f3b1e9c24";
	private static final String FAILED_WITH_5XX_KEY = "d8e6f4a2-7c5b-4e3d-9f1a-0b2c4d6e8f53";
	private static final String RESERVE_NOT_NULL_FAILURE_KEY = "f2a4c6e8-1d3b-4f5a-9c7e-8b6d4f2a0e17";
	private static final String RESERVE_CHECK_FAILURE_KEY = "07c9e1b3-5a7d-4c2f-8e0a-6d4b2f9c1e38";
	private static final String TEST_CHECK_CONSTRAINT = "ck_idempotency_key_test_reserve_failure";
	private static final String CHECK_DETAIL_LEAK_KEY = "detail-leak-54-check";
	private static final String NOT_NULL_DETAIL_LEAK_KEY = "detail-leak-54-not-null";
	private static final String DETAIL_LEAK_CHECK_CONSTRAINT = "ck_idempotency_key_test_no_leak_marker";
	private static final String FAILING_ROW_DETAIL = "Failing row contains";
	private static final String RESERVE_TRANSACTION_FAILURE_KEY = "1e3a5c7e-9b0d-4f2a-8c4e-6a8c0e2f4b59";
	private static final String LOOKUP_TRANSACTION_FAILURE_KEY = "5c7e9a1b-3d5f-4b7d-9e1a-2c4e6a8b0d73";
	private static final String TRANSACTION_FAILURE_DETAIL = "connection refused to idempotency-db:5432";
	private static final String STORAGE_FAILURE_LOG_MESSAGE = "Idempotency-Key storage failed";
	private static final String EMPTY_KEY = "";
	private static final String WHITESPACE_ONLY_KEY = "   ";
	private static final int MAX_KEY_LENGTH = 255;
	private static final String TOO_LONG_KEY = "k".repeat(MAX_KEY_LENGTH + 1);
	private static final String CONTAINS_KOREAN = "(?s).*[가-힣].*";
	private static final int RETRY_AFTER_SECONDS = 30;
	private static final long TIMEOUT_SECONDS = 10;
	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;
	private static final String FROM_STORED_USER_KEY_ROW = """
			from idempotency_key
			where principal_type = 'USER' and principal_id = ? and idempotency_key = ?""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;
	private final JdbcTemplate jdbcTemplate;
	private final IdempotentOrderTestController idempotentOrderTestController;
	private final Deque<String> schemaRestoreStatements = new ArrayDeque<>();
	@MockitoSpyBean
	private IdempotencyKeyStore idempotencyKeyStore;
	private String token;
	private int createCallsBefore;

	@BeforeEach
	void setUp() {
		token = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);
		createCallsBefore = idempotentOrderTestController.createCallCount();
	}

	@AfterEach
	void cleanUp() {
		try {
			restoreSchema();
		} finally {
			idempotentOrderTestController.clearHeldCall();
			idempotentOrderTestController.clearNextFailure();
			jdbcTemplate.update(
					"delete from idempotency_key where idempotency_key in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					KEY, OTHER_KEY, CONCURRENT_KEY, REUSED_WITH_OTHER_BODY_KEY, REUSED_ON_OTHER_PATH_KEY,
					NON_TARGET_KEY, FAILED_WITH_4XX_KEY, FAILED_WITH_5XX_KEY, RESERVE_NOT_NULL_FAILURE_KEY,
					RESERVE_CHECK_FAILURE_KEY, EMPTY_KEY, WHITESPACE_ONLY_KEY, CHECK_DETAIL_LEAK_KEY,
					NOT_NULL_DETAIL_LEAK_KEY, RESERVE_TRANSACTION_FAILURE_KEY, LOOKUP_TRANSACTION_FAILURE_KEY);
		}
	}

	@Test
	@DisplayName("Idempotency-Key 대상 POST API에 USER 101이 같은 키 · 같은 본문을 두 번 보내면 두 번째 응답은 첫 응답과 같은 상태 201 · 같은 JSON 본문(호출마다 늘어나는 orderNumber까지 같음)이다")
	void secondRequestWithSameKeyAndSameBodyReturnsFirstStatusAndBody() throws Exception {
		MvcResult first = mockMvc.perform(idempotentOrder(KEY))
				.andExpect(status().isCreated())
				.andReturn();
		MvcResult second = mockMvc.perform(idempotentOrder(KEY))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();

		JSONAssert.assertEquals(bodyOf(first), bodyOf(second), JSONCompareMode.STRICT);
	}

	@Test
	@DisplayName("같은 USER 101이 같은 본문이라도 다른 Idempotency-Key로 보내면 첫 응답을 돌려주지 않고 새로 처리한 201 응답(다른 orderNumber)을 돌려준다")
	void requestWithOtherKeyReturnsNewlyProcessedResponse() throws Exception {
		MvcResult first = mockMvc.perform(idempotentOrder(KEY))
				.andExpect(status().isCreated())
				.andReturn();
		MvcResult other = mockMvc.perform(idempotentOrder(OTHER_KEY))
				.andExpect(status().isCreated())
				.andReturn();

		assertThat(orderNumberOf(other)).isNotEqualTo(orderNumberOf(first));
	}

	@Test
	@DisplayName("Idempotency-Key 대상 POST API에 USER 101이 같은 키 · 같은 본문을 차례로 세 번 보내면 유스케이스 역할의 컨트롤러 메서드는 정확히 한 번만 실행된다")
	void sameKeyAndSameBodySentThreeTimesRunsUseCaseOnlyOnce() throws Exception {
		mockMvc.perform(idempotentOrder(KEY)).andExpect(status().isCreated());
		mockMvc.perform(idempotentOrder(KEY)).andExpect(status().isCreated());
		mockMvc.perform(idempotentOrder(KEY)).andExpect(status().isCreated());

		assertThat(createCallsDuringTest()).isEqualTo(1);
	}

	@Test
	@DisplayName("USER 101이 같은 Idempotency-Key · 같은 본문으로 두 요청을 동시에 보내 첫 요청이 컨트롤러 안에서 처리 중일 때 들어온 두 번째 요청은 컨트롤러를 실행하지 않고 409 IDEMPOTENCY_KEY_REUSED 공통 오류를 받고 · 첫 요청은 201이며 컨트롤러 메서드는 정확히 한 번만 실행된다")
	void concurrentSecondRequestWithSameKeyWhileFirstIsProcessingGets409AndUseCaseRunsOnce() throws Exception {
		HeldCall heldCall = idempotentOrderTestController.holdNextHeldCall();
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<MvcResult> first = executor.submit(
					() -> mockMvc.perform(heldIdempotentOrder(CONCURRENT_KEY)).andReturn());
			assertThat(heldCall.awaitEntered())
					.as("첫 요청이 %d초 안에 컨트롤러에 들어와 붙잡혀야 한다", TIMEOUT_SECONDS)
					.isTrue();

			Future<MvcResult> second = executor.submit(
					() -> mockMvc.perform(heldIdempotentOrder(CONCURRENT_KEY)).andReturn());
			MvcResult secondResult = second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			commonError(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED").match(secondResult);

			heldCall.release();
			MvcResult firstResult = first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			status().isCreated().match(firstResult);
			assertThat(createCallsDuringTest()).isEqualTo(1);
		} finally {
			heldCall.release();
			executor.shutdown();
			executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
	}

	@Test
	@DisplayName("USER 101이 Idempotency-Key 대상 POST API에 키 K · 본문 A로 201을 받은 뒤 같은 키 K · 다른 본문 B를 보내면 409 IDEMPOTENCY_KEY_REUSED 공통 오류를 받고 컨트롤러 메서드는 첫 요청 때 한 번만 실행되며 저장된 행의 요청 해시는 첫 요청 것 그대로다")
	void sameKeyWithOtherBodyGets409AndKeepsFirstRequestHash() throws Exception {
		mockMvc.perform(idempotentOrderTo(IDEMPOTENT_ORDER_PATH, REUSED_WITH_OTHER_BODY_KEY, REQUEST_BODY))
				.andExpect(status().isCreated());
		String firstRequestHash = storedRequestHashOf(REUSED_WITH_OTHER_BODY_KEY);

		mockMvc.perform(idempotentOrderTo(IDEMPOTENT_ORDER_PATH, REUSED_WITH_OTHER_BODY_KEY, OTHER_REQUEST_BODY))
				.andExpect(commonError(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED"));

		assertThat(createCallsDuringTest()).isEqualTo(1);
		assertThat(storedRequestHashOf(REUSED_WITH_OTHER_BODY_KEY)).isEqualTo(firstRequestHash);
	}

	@Test
	@DisplayName("USER 101이 Idempotency-Key 대상 API A에 키 K로 201을 받은 뒤 같은 키 K · 같은 본문을 다른 대상 API B로 보내면 경로가 요청 해시에 들어가므로 409 IDEMPOTENCY_KEY_REUSED 공통 오류를 받고 컨트롤러 메서드는 한 번만 실행되며 저장된 행의 요청 해시는 첫 요청 것 그대로다")
	void sameKeyWithSameBodyOnOtherTargetPathGets409AndKeepsFirstRequestHash() throws Exception {
		mockMvc.perform(idempotentOrderTo(IDEMPOTENT_ORDER_PATH, REUSED_ON_OTHER_PATH_KEY, REQUEST_BODY))
				.andExpect(status().isCreated());
		String firstRequestHash = storedRequestHashOf(REUSED_ON_OTHER_PATH_KEY);

		mockMvc.perform(idempotentOrderTo(HELD_IDEMPOTENT_ORDER_PATH, REUSED_ON_OTHER_PATH_KEY, REQUEST_BODY))
				.andExpect(commonError(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED"));

		assertThat(createCallsDuringTest()).isEqualTo(1);
		assertThat(storedRequestHashOf(REUSED_ON_OTHER_PATH_KEY)).isEqualTo(firstRequestHash);
	}

	@Test
	@DisplayName("USER 101이 Idempotency-Key 대상 POST API를 Idempotency-Key 헤더 없이 부르면 422 VALIDATION_FAILED와 fields[0].field Idempotency-Key · fields[0].reason 한국어 문구를 받고 컨트롤러 메서드는 실행되지 않는다")
	void requestWithoutIdempotencyKeyHeaderToTargetApiGets422AndUseCaseDoesNotRun() throws Exception {
		mockMvc.perform(orderWithoutKeyTo(IDEMPOTENT_ORDER_PATH))
				.andExpect(idempotencyKeyValidationFailed());

		assertThat(createCallsDuringTest()).isZero();
	}

	@Test
	@DisplayName("USER 101이 Idempotency-Key 대상 POST API에 256자 Idempotency-Key를 보내면 409나 DB 오류가 아니라 422 VALIDATION_FAILED와 fields[0].field Idempotency-Key를 받고 컨트롤러 메서드는 실행되지 않는다")
	void idempotencyKeyLongerThan255CharsGets422AndUseCaseDoesNotRun() throws Exception {
		mockMvc.perform(idempotentOrder(TOO_LONG_KEY))
				.andExpect(idempotencyKeyValidationFailed());

		assertThat(createCallsDuringTest()).isZero();
	}

	@ParameterizedTest(name = "키 [{0}]")
	@ValueSource(strings = {EMPTY_KEY, WHITESPACE_ONLY_KEY})
	@DisplayName("USER 101이 Idempotency-Key 대상 POST API에 빈 문자열이나 공백뿐인 Idempotency-Key를 보내면 422 VALIDATION_FAILED와 fields[0].field Idempotency-Key를 받고 컨트롤러 메서드는 실행되지 않는다")
	void blankIdempotencyKeyGets422AndUseCaseDoesNotRun(String blankKey) throws Exception {
		mockMvc.perform(idempotentOrder(blankKey))
				.andExpect(idempotencyKeyValidationFailed());

		assertThat(createCallsDuringTest()).isZero();
	}

	@Test
	@DisplayName("USER 101이 @IdempotencyKeyRequired가 없는 POST API를 Idempotency-Key 헤더 없이 부르면 422가 아니라 201로 처리되고 컨트롤러 메서드가 한 번 실행된다")
	void nonTargetApiWithoutIdempotencyKeyHeaderIsProcessed() throws Exception {
		mockMvc.perform(orderWithoutKeyTo(NON_TARGET_ORDER_PATH))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderNumber").isNumber());

		assertThat(createCallsDuringTest()).isEqualTo(1);
	}

	@Test
	@DisplayName("USER 101이 @IdempotencyKeyRequired가 없는 POST API에 같은 Idempotency-Key · 같은 본문을 두 번 보내면 첫 응답을 돌려주지 않고 두 번 모두 새로 처리해(다른 orderNumber · 컨트롤러 메서드 2번 실행) idempotency_key 행을 만들지 않는다")
	void nonTargetApiWithSameIdempotencyKeyTwiceIsProcessedEachTimeAndStoresNoKey() throws Exception {
		MvcResult first = mockMvc.perform(nonTargetOrder(NON_TARGET_KEY))
				.andExpect(status().isCreated())
				.andReturn();
		MvcResult second = mockMvc.perform(nonTargetOrder(NON_TARGET_KEY))
				.andExpect(status().isCreated())
				.andReturn();

		assertThat(orderNumberOf(second)).isNotEqualTo(orderNumberOf(first));
		assertThat(createCallsDuringTest()).isEqualTo(2);
		assertThat(storedKeyCountOf(NON_TARGET_KEY)).isZero();
	}

	@Test
	@DisplayName("USER 101이 Idempotency-Key 대상 POST API에 키 K로 보낸 첫 요청이 공통 예외 429 TOO_MANY_REQUESTS로 끝나면 키 K 행이 남지 않고 같은 키 K · 같은 본문으로 다시 보내면 컨트롤러 메서드가 다시 실행되어(2번 실행) 201을 받으며 그 뒤 키 K 행에는 201이 저장된다")
	void targetApiEndedWith4xxStoresNoKeyAndRetryWithSameKeyRunsUseCaseAgain() throws Exception {
		idempotentOrderTestController.failNextFailingOnceCall(BusinessException.tooManyRequests(RETRY_AFTER_SECONDS));

		mockMvc.perform(failingOnceIdempotentOrder(FAILED_WITH_4XX_KEY))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
				.andExpect(jsonPath("$.error.retryAfterSeconds").value(RETRY_AFTER_SECONDS));
		assertThat(storedKeyCountOf(FAILED_WITH_4XX_KEY)).isZero();

		mockMvc.perform(failingOnceIdempotentOrder(FAILED_WITH_4XX_KEY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderNumber").isNumber());

		assertThat(createCallsDuringTest()).isEqualTo(2);
		assertThat(storedResponseStatusOf(FAILED_WITH_4XX_KEY)).isEqualTo(HttpStatus.CREATED.value());
	}

	@Test
	@DisplayName("USER 101이 Idempotency-Key 대상 POST API에 키 K로 보낸 첫 요청이 처리하지 않은 예외로 500 INTERNAL_SERVER_ERROR로 끝나면 키 K 행이 남지 않고 같은 키 K · 같은 본문으로 다시 보내면 컨트롤러 메서드가 다시 실행되어(2번 실행) 201을 받으며 그 뒤 키 K 행에는 201이 저장된다")
	void targetApiEndedWith5xxStoresNoKeyAndRetryWithSameKeyRunsUseCaseAgain() throws Exception {
		idempotentOrderTestController.failNextFailingOnceCall(new IllegalStateException("테스트용 처리 실패"));

		mockMvc.perform(failingOnceIdempotentOrder(FAILED_WITH_5XX_KEY))
				.andExpect(commonError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"));
		assertThat(storedKeyCountOf(FAILED_WITH_5XX_KEY)).isZero();

		mockMvc.perform(failingOnceIdempotentOrder(FAILED_WITH_5XX_KEY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderNumber").isNumber());

		assertThat(createCallsDuringTest()).isEqualTo(2);
		assertThat(storedResponseStatusOf(FAILED_WITH_5XX_KEY)).isEqualTo(HttpStatus.CREATED.value());
	}

	@Test
	@DisplayName("옛 dev DB처럼 idempotency_key.response_status에 NOT NULL 제약이 남아 키 예약 INSERT가 NOT NULL 위반으로 실패하면 USER 101의 Idempotency-Key 대상 POST API 요청은 409 IDEMPOTENCY_KEY_REUSED가 아니라 500 INTERNAL_SERVER_ERROR 공통 오류(본문에 테이블 · 컬럼 이름 없음)를 받고 컨트롤러 메서드는 실행되지 않으며 키 행도 남지 않는다")
	void reservationFailingWithNotNullViolationEndsWith500AndUseCaseDoesNotRun() throws Exception {
		alterSchemaUntilTestEnds(
				"alter table idempotency_key alter column response_status set not null",
				"alter table idempotency_key alter column response_status drop not null");

		assertReservationFailureEndsWithInternalServerError(RESERVE_NOT_NULL_FAILURE_KEY);
	}

	@Test
	@DisplayName("idempotency_key에 유니크가 아닌 CHECK 제약이 걸려 키 예약 INSERT가 CHECK 위반으로 실패하면 USER 101의 Idempotency-Key 대상 POST API 요청은 409 IDEMPOTENCY_KEY_REUSED가 아니라 500 INTERNAL_SERVER_ERROR 공통 오류(본문에 테이블 · 제약 이름 없음)를 받고 컨트롤러 메서드는 실행되지 않으며 키 행도 남지 않는다")
	void reservationFailingWithCheckViolationEndsWith500AndUseCaseDoesNotRun() throws Exception {
		alterSchemaUntilTestEnds(
				"alter table idempotency_key add constraint " + TEST_CHECK_CONSTRAINT
						+ " check (idempotency_key <> '" + RESERVE_CHECK_FAILURE_KEY + "') not valid",
				"alter table idempotency_key drop constraint if exists " + TEST_CHECK_CONSTRAINT);

		assertReservationFailureEndsWithInternalServerError(RESERVE_CHECK_FAILURE_KEY);
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	@DisplayName("키 detail-leak-54-check의 예약 INSERT가 CHECK 제약 위반으로 실패해 500으로 끝나도 그 동안 콘솔에 찍힌 로그 어디에도 PostgreSQL 오류 상세 Failing row contains와 그 행 값(키 detail-leak-54-check)이 없다")
	void checkViolationOnReservationLeavesNoFailingRowDetailInLogs(CapturedOutput output) throws Exception {
		alterSchemaUntilTestEnds(
				"alter table idempotency_key add constraint " + DETAIL_LEAK_CHECK_CONSTRAINT
						+ " check (position('leak' in idempotency_key) = 0) not valid",
				"alter table idempotency_key drop constraint if exists " + DETAIL_LEAK_CHECK_CONSTRAINT);

		assertReservationFailureLogsNoFailingRowDetail(output, CHECK_DETAIL_LEAK_KEY);
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	@DisplayName("키 detail-leak-54-not-null의 예약 INSERT가 response_status NOT NULL 제약 위반으로 실패해 500으로 끝나도 그 동안 콘솔에 찍힌 로그 어디에도 PostgreSQL 오류 상세 Failing row contains와 그 행 값(키 detail-leak-54-not-null)이 없다")
	void notNullViolationOnReservationLeavesNoFailingRowDetailInLogs(CapturedOutput output) throws Exception {
		alterSchemaUntilTestEnds(
				"alter table idempotency_key alter column response_status set not null",
				"alter table idempotency_key alter column response_status drop not null");

		assertReservationFailureLogsNoFailingRowDetail(output, NOT_NULL_DETAIL_LEAK_KEY);
	}

	@Test
	@DisplayName("키 예약 단계에서 DataAccessException이 아닌 트랜잭션 시작 실패(CannotCreateTransactionException)가 나면 USER 101의 Idempotency-Key 대상 POST API 요청은 예외가 필터 밖으로 새지 않고 500 INTERNAL_SERVER_ERROR 공통 오류(본문에 예외 메시지 · 예외 클래스 이름 없음)를 받고 컨트롤러 메서드는 실행되지 않으며 키 행도 남지 않는다")
	void reservationFailingWithTransactionStartFailureEndsWith500AndUseCaseDoesNotRun() throws Exception {
		doThrow(new CannotCreateTransactionException(TRANSACTION_FAILURE_DETAIL))
				.when(idempotencyKeyStore).reserve(any());

		assertNonDataAccessStorageFailureEndsWithInternalServerError(RESERVE_TRANSACTION_FAILURE_KEY);
	}

	@Test
	@DisplayName("같은 키 행이 있어 예약이 비고 이어진 기존 행 조회에서 DataAccessException이 아닌 트랜잭션 시작 실패(CannotCreateTransactionException)가 나면 USER 101의 Idempotency-Key 대상 POST API 요청은 예외가 필터 밖으로 새지 않고 500 INTERNAL_SERVER_ERROR 공통 오류(본문에 예외 메시지 · 예외 클래스 이름 없음)를 받고 컨트롤러 메서드는 실행되지 않는다")
	void existingKeyLookupFailingWithTransactionStartFailureEndsWith500AndUseCaseDoesNotRun() throws Exception {
		doReturn(Optional.empty()).when(idempotencyKeyStore).reserve(any());
		doThrow(new CannotCreateTransactionException(TRANSACTION_FAILURE_DETAIL))
				.when(idempotencyKeyStore).findReplayable(any());

		assertNonDataAccessStorageFailureEndsWithInternalServerError(LOOKUP_TRANSACTION_FAILURE_KEY);
	}

	private void assertNonDataAccessStorageFailureEndsWithInternalServerError(String idempotencyKey)
			throws Exception {
		mockMvc.perform(idempotentOrder(idempotencyKey))
				.andExpect(commonError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"))
				.andExpect(content().string(allOf(
						not(containsString(TRANSACTION_FAILURE_DETAIL)),
						not(containsString("CannotCreateTransactionException")),
						not(containsString("org.springframework")))));

		assertThat(createCallsDuringTest()).isZero();
		assertThat(storedKeyCountOf(idempotencyKey)).isZero();
	}

	private void assertReservationFailureLogsNoFailingRowDetail(CapturedOutput output, String markerKey)
			throws Exception {
		mockMvc.perform(idempotentOrder(markerKey))
				.andExpect(commonError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"));

		String consoleOutput = output.getAll();
		assertThat(consoleOutput)
				.as("콘솔 출력이 잡혀야 한다: 예약 실패 error 로그가 콘솔에 있어야 한다")
				.contains(STORAGE_FAILURE_LOG_MESSAGE);
		assertThat(consoleOutput)
				.as("로그에 PostgreSQL 오류 상세가 남으면 안 된다")
				.doesNotContain(FAILING_ROW_DETAIL);
		assertThat(consoleOutput)
				.as("로그에 실패한 행 값(키 %s)이 남으면 안 된다", markerKey)
				.doesNotContain(markerKey);
	}

	private void assertReservationFailureEndsWithInternalServerError(String idempotencyKey) throws Exception {
		mockMvc.perform(idempotentOrder(idempotencyKey))
				.andExpect(commonError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"))
				.andExpect(content().string(allOf(
						not(containsString("idempotency_key")),
						not(containsString("response_status")),
						not(containsString(TEST_CHECK_CONSTRAINT)))));

		assertThat(createCallsDuringTest()).isZero();
		assertThat(storedKeyCountOf(idempotencyKey)).isZero();
	}

	private void alterSchemaUntilTestEnds(String alterStatement, String restoreStatement) {
		schemaRestoreStatements.push(restoreStatement);
		jdbcTemplate.execute(alterStatement);
	}

	private void restoreSchema() {
		while (!schemaRestoreStatements.isEmpty()) {
			jdbcTemplate.execute(schemaRestoreStatements.pop());
		}
	}

	private int createCallsDuringTest() {
		return idempotentOrderTestController.createCallCount() - createCallsBefore;
	}

	private static ResultMatcher idempotencyKeyValidationFailed() {
		return result -> {
			status().isUnprocessableContent().match(result);
			commonErrorKeys().match(result);
			jsonPath("$.error.code").value("VALIDATION_FAILED").match(result);
			jsonPath("$.error.message").value(VALIDATION_FAILED_MESSAGE).match(result);
			jsonPath("$.error.fields", hasSize(1)).match(result);
			jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER).match(result);
			jsonPath("$.error.fields[0].reason", matchesPattern(CONTAINS_KOREAN)).match(result);
		};
	}

	private MockHttpServletRequestBuilder idempotentOrder(String idempotencyKey) {
		return idempotentOrderTo(IDEMPOTENT_ORDER_PATH, idempotencyKey, REQUEST_BODY);
	}

	private MockHttpServletRequestBuilder heldIdempotentOrder(String idempotencyKey) {
		return idempotentOrderTo(HELD_IDEMPOTENT_ORDER_PATH, idempotencyKey, REQUEST_BODY);
	}

	private MockHttpServletRequestBuilder failingOnceIdempotentOrder(String idempotencyKey) {
		return idempotentOrderTo(FAILING_ONCE_IDEMPOTENT_ORDER_PATH, idempotencyKey, REQUEST_BODY);
	}

	private MockHttpServletRequestBuilder nonTargetOrder(String idempotencyKey) {
		return idempotentOrderTo(NON_TARGET_ORDER_PATH, idempotencyKey, REQUEST_BODY);
	}

	private MockHttpServletRequestBuilder idempotentOrderTo(String path, String idempotencyKey, String requestBody) {
		return orderTo(path, requestBody).header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
	}

	private MockHttpServletRequestBuilder orderWithoutKeyTo(String path) {
		return orderTo(path, REQUEST_BODY);
	}

	private MockHttpServletRequestBuilder orderTo(String path, String requestBody) {
		return post(path)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody.getBytes(StandardCharsets.UTF_8));
	}

	private String storedRequestHashOf(String idempotencyKey) {
		return jdbcTemplate.queryForObject("select request_hash " + FROM_STORED_USER_KEY_ROW,
				String.class, USER_ID, idempotencyKey);
	}

	private Integer storedResponseStatusOf(String idempotencyKey) {
		return jdbcTemplate.queryForObject("select response_status " + FROM_STORED_USER_KEY_ROW,
				Integer.class, USER_ID, idempotencyKey);
	}

	private long storedKeyCountOf(String idempotencyKey) {
		Long count = jdbcTemplate.queryForObject(
				"select count(*) from idempotency_key where idempotency_key = ?", Long.class, idempotencyKey);
		return count == null ? 0 : count;
	}

	private static long orderNumberOf(MvcResult result) throws Exception {
		Number orderNumber = JsonPath.read(bodyOf(result), "$.orderNumber");
		return orderNumber.longValue();
	}

	static final class HeldCall {

		private final CountDownLatch entered = new CountDownLatch(1);
		private final CountDownLatch released = new CountDownLatch(1);

		void holdUntilReleased() throws InterruptedException {
			entered.countDown();
			released.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}

		boolean awaitEntered() throws InterruptedException {
			return entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}

		void release() {
			released.countDown();
		}

	}

	@RestController
	static class IdempotentOrderTestController {

		private final AtomicLong orderNumbers = new AtomicLong();
		private final AtomicInteger createCalls = new AtomicInteger();
		private final AtomicReference<HeldCall> nextHeldCall = new AtomicReference<>();
		private final AtomicReference<RuntimeException> nextFailure = new AtomicReference<>();

		@IdempotencyKeyRequired
		@PostMapping(IDEMPOTENT_ORDER_PATH)
		public ResponseEntity<OrderResponse> create(@RequestBody OrderRequest request) {
			createCalls.incrementAndGet();
			OrderResponse response = OrderResponse.of(orderNumbers.incrementAndGet(), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		}

		@IdempotencyKeyRequired
		@PostMapping(HELD_IDEMPOTENT_ORDER_PATH)
		public ResponseEntity<OrderResponse> createHeld(@RequestBody OrderRequest request)
				throws InterruptedException {
			HeldCall heldCall = nextHeldCall.getAndSet(null);
			if (heldCall != null) {
				heldCall.holdUntilReleased();
			}
			return create(request);
		}

		@IdempotencyKeyRequired
		@PostMapping(FAILING_ONCE_IDEMPOTENT_ORDER_PATH)
		public ResponseEntity<OrderResponse> createFailingOnce(@RequestBody OrderRequest request) {
			RuntimeException failure = nextFailure.getAndSet(null);
			if (failure != null) {
				createCalls.incrementAndGet();
				throw failure;
			}
			return create(request);
		}

		@PostMapping(NON_TARGET_ORDER_PATH)
		public ResponseEntity<OrderResponse> createWithoutIdempotencyKey(@RequestBody OrderRequest request) {
			return create(request);
		}

		public int createCallCount() {
			return createCalls.get();
		}

		HeldCall holdNextHeldCall() {
			HeldCall heldCall = new HeldCall();
			nextHeldCall.set(heldCall);
			return heldCall;
		}

		void clearHeldCall() {
			nextHeldCall.set(null);
		}

		void failNextFailingOnceCall(RuntimeException failure) {
			nextFailure.set(failure);
		}

		void clearNextFailure() {
			nextFailure.set(null);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(IdempotentOrderTestController.class)
	static class IdempotentOrderTestEndpointConfiguration {
	}

}

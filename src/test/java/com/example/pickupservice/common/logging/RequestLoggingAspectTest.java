package com.example.pickupservice.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class RequestLoggingAspectTest extends IntegrationTestSupport {

	private static final String PING_PATH = "/test/logging/ping";
	private static final String SLOW_PATH = "/test/logging/slow";
	private static final String ITEMS_PATH = "/test/logging/items";
	private static final String PUBLIC_ENDPOINT_PATTERN = "/test/logging-public/**";
	private static final String PUBLIC_PING_PATH = "/test/logging-public/ping";
	private static final String BUSINESS_ERROR_PATH = "/test/logging/business-error";
	private static final String UNEXPECTED_ERROR_PATH = "/test/logging/unexpected-error";
	private static final String VALIDATED_PATH = "/test/logging/validated";
	private static final String DOMAIN_ERROR_CODE = "SOME_DOMAIN_CODE";
	private static final String DOMAIN_ERROR_MESSAGE = "도메인 규칙에 맞지 않는 요청입니다";
	private static final String INTERNAL_DETAIL = "internal-detail-secret-5678 failed in InternalRepository.load";
	private static final String BLANK_NAME_BODY = "{\"name\": \"\"}";
	private static final String ERROR_CODE_KEY = "errorCode";
	private static final long USER_ID = 101L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long SLOW_HANDLER_SLEEP_MILLIS = 60L;
	private static final long MIN_LOGGED_SLOW_DURATION_MILLIS = 50L;
	private static final Pattern LOG_FIELD = Pattern.compile("([A-Za-z]+)=(\\S+)");
	private static final String ANNOTATED_CREATED_PATH = "/test/logging/annotated-created";
	private static final long CREATED_ITEM_ID = 501L;
	private static final String GUARDIAN_ONLY_PATH = "/test/logging/guardian-only";
	private static final String FORBIDDEN_CODE = "FORBIDDEN";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";
	private static final String SAFE_REQUEST_ID = "req-Abc_123";
	private static final String SIXTY_FOUR_CHAR_SAFE_REQUEST_ID = "req-" + "a".repeat(30) + "_" + "B".repeat(28) + "9";
	private static final String SIXTY_FIVE_CHAR_REQUEST_ID = "a".repeat(65);
	private static final String FORGED_REQUEST_ID = "forged-request-id";
	private static final String FORGED_PRINCIPAL = "USER:999";
	private static final Pattern UUID_FORMAT =
			Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	private static final String SENSITIVE_BODY_PATH = "/test/logging/sensitive-body";
	private static final String STRICT_SENSITIVE_BODY_PATH = "/test/logging/strict-sensitive-body";
	private static final String SENSITIVE_QUERY_PATH = "/test/logging/sensitive-query";
	private static final String SENSITIVE_PHONE = "010-1234-5678";
	private static final String SENSITIVE_LAT = "37.501234";
	private static final String SENSITIVE_LNG = "127.039876";
	private static final String SENSITIVE_BODY_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJib2R5In0.body-token-secret";
	private static final String SENSITIVE_LINK_CODE = "LINK-9F3K2";
	private static final String[] SENSITIVE_BODY_VALUES =
			{SENSITIVE_PHONE, SENSITIVE_LAT, SENSITIVE_LNG, SENSITIVE_BODY_TOKEN, SENSITIVE_LINK_CODE};
	private static final String SENSITIVE_BODY = "{\"phone\":\"%s\",\"lat\":%s,\"lng\":%s,\"token\":\"%s\",\"linkCode\":\"%s\"}"
			.formatted(SENSITIVE_PHONE, SENSITIVE_LAT, SENSITIVE_LNG, SENSITIVE_BODY_TOKEN, SENSITIVE_LINK_CODE);
	private static final String QUERY_PHONE = "01012345678";
	private static final String QUERY_LINK_CODE = "LINK-9F3K2";
	private static final String STREAM_PATH = "/test/logging/stream";
	private static final String STREAM_EVENT_NAME = "CALL_SENT";
	private static final int STREAM_EVENT_COUNT = 3;
	private static final long STREAM_TIMEOUT_MILLIS = 30_000L;
	private static final long STREAM_HOLD_MILLIS = 60L;
	private static final long MIN_LOGGED_STREAM_DURATION_MILLIS = 50L;
	private static final String STREAM_KEY = "stream";
	private static final String STREAM_OPEN = "open";
	private static final String STREAM_CLOSED = "closed";
	private static final String POLLING_PATH_TEMPLATE = "/test/logging/link-codes/{codeId}/status";
	private static final String POLL_STATE_FIELD = "status";
	private static final String WAITING = "WAITING";
	private static final String LINKED = "LINKED";
	private static final String LINK_CODE_INVALID = "LINK_CODE_INVALID";
	private static final String LINK_CODE_INVALID_MESSAGE = "연결 코드를 찾을 수 없습니다";
	private static final int WAITING_POLL_AFTER_SECONDS = 2;
	private static final long CHANGING_CODE_ID = 9001L;
	private static final long FIRST_INTERLEAVED_CODE_ID = 9101L;
	private static final long SECOND_INTERLEAVED_CODE_ID = 9102L;
	private static final long INVALIDATED_CODE_ID = 9201L;
	private static final String INVITE_PATH_PATTERN = "/test/logging/invites/{token}";
	private static final String INVITE_TOKEN = "inv-7Hq2-secret";
	private static final String VEHICLE_LINK_PATH_PATTERN = "/test/logging/vehicle-links/{linkCode}";
	private static final String PATH_LINK_CODE = "LINK-7Q2W9";
	private static final String TOKEN_STREAM_PATH_PATTERN = "/test/logging/streams/{streamToken}";
	private static final String STREAM_TOKEN = "stream-K3x9-secret";
	private static final String CONSOLE_CHECK_REQUEST_ID = "req-51-check";
	private static final String UNHANDLED_EXCEPTION_LOG_MESSAGE = "Unhandled exception";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;
	private final TestSseConnections sseConnections;
	private final ScriptedLinkCodeStatuses linkCodeStatuses;

	private ListAppender<ILoggingEvent> logCapture;

	@BeforeEach
	void attachLogCapture() {
		logCapture = new ListAppender<>();
		logCapture.start();
		rootLogger().addAppender(logCapture);
	}

	@AfterEach
	void detachLogCapture() {
		rootLogger().detachAppender(logCapture);
		logCapture.stop();
	}

	@Test
	@DisplayName("USER 101 토큰으로 GET /test/logging/ping을 부르면 그 요청의 로그가 INFO 한 줄만 남고 그 줄에 requestId · principal=USER:101 · method=GET · path=/test/logging/ping · status=200 · durationMs(숫자)가 모두 들어 있다")
	void userGetRequestLeavesExactlyOneInfoLineWithAllRequestFields() throws Exception {
		mockMvc.perform(asUser(get(PING_PATH)))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		ILoggingEvent requestLog = requestLogs.getFirst();
		assertThat(requestLog.getLevel()).isEqualTo(Level.INFO);
		assertThat(requestLog.getFormattedMessage()).doesNotContain("\n", "\r");
		Map<String, String> fields = fieldsOf(requestLog);
		assertThat(fields.get("requestId")).isNotBlank();
		assertThat(fields)
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "GET")
				.containsEntry("path", PING_PATH)
				.containsEntry("status", "200");
		assertThat(fields.get("durationMs")).isNotNull().matches("\\d+");
	}

	@Test
	@DisplayName("VEHICLE 77 토큰으로 GET /test/logging/ping을 부르면 그 요청 로그 한 줄의 호출 주체가 principal=VEHICLE:77이다")
	void vehicleGetRequestLogsVehiclePrincipal() throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);

		mockMvc.perform(get(PING_PATH).header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken)))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst()))
				.containsEntry("principal", "VEHICLE:77")
				.containsEntry("method", "GET")
				.containsEntry("status", "200");
	}

	@Test
	@DisplayName("인증 없이 부르는 API GET /test/logging-public/ping을 토큰 없이 부르면 그 요청 로그 한 줄의 호출 주체가 principal=ANONYMOUS이고 status=200이다")
	void anonymousGetRequestLogsAnonymousPrincipal() throws Exception {
		mockMvc.perform(get(PUBLIC_PING_PATH))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PUBLIC_PING_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst()))
				.containsEntry("principal", "ANONYMOUS")
				.containsEntry("method", "GET")
				.containsEntry("status", "200");
	}

	@Test
	@DisplayName("USER 101 토큰으로 POST /test/logging/items를 불러 컨트롤러가 ResponseEntity 201을 돌려주면 그 요청 로그 한 줄에 method=POST · status=201이 남는다")
	void postRequestReturningCreatedLogsMethodPostAndStatus201() throws Exception {
		mockMvc.perform(asUser(post(ITEMS_PATH)))
				.andExpect(status().isCreated());

		List<ILoggingEvent> requestLogs = requestLogsFor(ITEMS_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst()))
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "POST")
				.containsEntry("status", "201");
	}

	@Test
	@DisplayName("컨트롤러 메서드가 60ms 걸리는 GET /test/logging/slow를 부르면 그 요청 로그 한 줄의 durationMs가 50 이상이다")
	void slowRequestLogsMeasuredDurationInMillis() throws Exception {
		mockMvc.perform(asUser(get(SLOW_PATH)))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(SLOW_PATH);
		assertThat(requestLogs).hasSize(1);
		String durationMs = fieldsOf(requestLogs.getFirst()).get("durationMs");
		assertThat(durationMs).isNotNull().matches("\\d+");
		assertThat(Long.parseLong(durationMs)).isGreaterThanOrEqualTo(MIN_LOGGED_SLOW_DURATION_MILLIS);
	}

	@Test
	@DisplayName("같은 USER 토큰으로 GET /test/logging/ping을 두 번 부르면 요청 로그가 두 줄 남고 두 줄의 requestId가 서로 다르다")
	void eachRequestLogsDifferentRequestId() throws Exception {
		mockMvc.perform(asUser(get(PING_PATH)))
				.andExpect(status().isOk());
		mockMvc.perform(asUser(get(PING_PATH)))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(2);
		String firstRequestId = fieldsOf(requestLogs.get(0)).get("requestId");
		String secondRequestId = fieldsOf(requestLogs.get(1)).get("requestId");
		assertThat(firstRequestId).isNotBlank();
		assertThat(secondRequestId).isNotBlank().isNotEqualTo(firstRequestId);
	}

	@Test
	@DisplayName("컨트롤러가 BusinessException(409 · SOME_DOMAIN_CODE)을 던지면 그 요청 로그가 한 줄 남고 그 줄에 status=409 · errorCode=SOME_DOMAIN_CODE와 requestId · principal · method · path · durationMs가 들어 있다")
	void businessExceptionRequestLogsActualStatusAndBusinessErrorCode() throws Exception {
		mockMvc.perform(asUser(get(BUSINESS_ERROR_PATH)))
				.andExpect(status().isConflict());

		List<ILoggingEvent> requestLogs = requestLogsFor(BUSINESS_ERROR_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(requestLogs.getFirst().getFormattedMessage()).doesNotContain("\n", "\r");
		Map<String, String> fields = fieldsOf(requestLogs.getFirst());
		assertThat(fields.get("requestId")).isNotBlank();
		assertThat(fields)
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "GET")
				.containsEntry("path", BUSINESS_ERROR_PATH)
				.containsEntry("status", "409")
				.containsEntry(ERROR_CODE_KEY, DOMAIN_ERROR_CODE);
		assertThat(fields.get("durationMs")).isNotNull().matches("\\d+");
	}

	@Test
	@DisplayName("컨트롤러가 처리하지 않은 RuntimeException을 던지면 그 요청 로그가 한 줄 남고 그 줄에 status=500 · errorCode=INTERNAL_SERVER_ERROR가 들어 있고 예외 메시지와 스택은 들어 있지 않다")
	void unexpectedExceptionRequestLogsStatus500AndInternalServerErrorWithoutExceptionDetail() throws Exception {
		mockMvc.perform(asUser(get(UNEXPECTED_ERROR_PATH)))
				.andExpect(status().isInternalServerError());

		List<ILoggingEvent> requestLogs = requestLogsFor(UNEXPECTED_ERROR_PATH);
		assertThat(requestLogs).hasSize(1);
		ILoggingEvent requestLog = requestLogs.getFirst();
		assertThat(fieldsOf(requestLog))
				.containsEntry("method", "GET")
				.containsEntry("status", "500")
				.containsEntry(ERROR_CODE_KEY, "INTERNAL_SERVER_ERROR");
		assertThat(requestLog.getFormattedMessage())
				.doesNotContain(INTERNAL_DETAIL, "IllegalStateException", "\n", "\r");
		assertThat(requestLog.getThrowableProxy()).isNull();
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	@DisplayName("X-Request-Id req-51-check로 부른 GET /test/logging/unexpected-error가 처리하지 않은 예외로 500이 되면 콘솔에 찍힌 500 처리기의 Unhandled exception 줄에도 requestId req-51-check가 찍힌다")
	void unhandledExceptionErrorLogLineOnConsoleCarriesRequestId(CapturedOutput output) throws Exception {
		mockMvc.perform(asUser(get(UNEXPECTED_ERROR_PATH)).header(REQUEST_ID_HEADER, CONSOLE_CHECK_REQUEST_ID))
				.andExpect(commonError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"));

		List<String> consoleLines = output.getAll().lines().toList();
		assertThat(consoleLines)
				.as("콘솔 출력이 잡혀야 한다: 그 요청의 요청 로그 줄이 콘솔에 있어야 한다")
				.anyMatch(line -> line.contains("requestId=" + CONSOLE_CHECK_REQUEST_ID)
						&& line.contains("path=" + UNEXPECTED_ERROR_PATH));
		List<String> unhandledExceptionLines = consoleLines.stream()
				.filter(line -> line.contains(UNHANDLED_EXCEPTION_LOG_MESSAGE))
				.toList();
		assertThat(unhandledExceptionLines)
				.as("500 처리기가 콘솔에 Unhandled exception 줄을 남겨야 한다")
				.isNotEmpty();
		assertThat(unhandledExceptionLines)
				.as("500 처리기의 Unhandled exception 줄에 MDC requestId가 찍혀야 한다")
				.allSatisfy(line -> assertThat(line).contains(CONSOLE_CHECK_REQUEST_ID));
	}

	@Test
	@DisplayName("@Valid 본문 검증에 실패한 POST /test/logging/validated 요청도 요청 로그가 한 줄 남고 그 줄에 method=POST · status=422 · errorCode=VALIDATION_FAILED가 들어 있다")
	void validationFailureRequestLogsStatus422AndValidationFailed() throws Exception {
		mockMvc.perform(asUser(post(VALIDATED_PATH))
						.contentType(MediaType.APPLICATION_JSON)
						.content(BLANK_NAME_BODY))
				.andExpect(status().isUnprocessableContent());

		List<ILoggingEvent> requestLogs = requestLogsFor(VALIDATED_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst()))
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "POST")
				.containsEntry("status", "422")
				.containsEntry(ERROR_CODE_KEY, "VALIDATION_FAILED");
	}

	@Test
	@DisplayName("예외로 끝난 요청 바로 뒤에 정상 요청 GET /test/logging/ping을 부르면 예외 요청 줄에는 errorCode가 있고 정상 요청 줄에는 errorCode 값이 없다")
	void successfulRequestAfterFailedRequestLogsNoErrorCode() throws Exception {
		mockMvc.perform(asUser(get(BUSINESS_ERROR_PATH)))
				.andExpect(status().isConflict());
		mockMvc.perform(asUser(get(PING_PATH)))
				.andExpect(status().isOk());

		List<ILoggingEvent> failedRequestLogs = requestLogsFor(BUSINESS_ERROR_PATH);
		List<ILoggingEvent> successfulRequestLogs = requestLogsFor(PING_PATH);
		assertThat(failedRequestLogs).hasSize(1);
		assertThat(fieldsOf(failedRequestLogs.getFirst())).containsEntry(ERROR_CODE_KEY, DOMAIN_ERROR_CODE);
		assertThat(successfulRequestLogs).hasSize(1);
		Map<String, String> successfulFields = fieldsOf(successfulRequestLogs.getFirst());
		assertThat(successfulFields).containsEntry("status", "200");
		assertThat(successfulFields).doesNotContainKey(ERROR_CODE_KEY);
	}

	@Test
	@DisplayName("안전한 X-Request-Id 헤더 req-Abc_123을 보내면 그 요청 로그 한 줄의 requestId가 req-Abc_123 그대로다")
	void safeRequestIdHeaderIsLoggedAsIs() throws Exception {
		mockMvc.perform(asUser(get(PING_PATH)).header(REQUEST_ID_HEADER, SAFE_REQUEST_ID))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst())).containsEntry("requestId", SAFE_REQUEST_ID);
	}

	@Test
	@DisplayName("영숫자 · - · _로만 된 딱 64자 X-Request-Id 헤더를 보내면 그 요청 로그의 requestId가 보낸 64자 값 그대로다")
	void sixtyFourCharacterSafeRequestIdHeaderIsLoggedAsIs() throws Exception {
		assertThat(SIXTY_FOUR_CHAR_SAFE_REQUEST_ID).hasSize(64);

		mockMvc.perform(asUser(get(PING_PATH)).header(REQUEST_ID_HEADER, SIXTY_FOUR_CHAR_SAFE_REQUEST_ID))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst())).containsEntry("requestId", SIXTY_FOUR_CHAR_SAFE_REQUEST_ID);
	}

	@Test
	@DisplayName("영숫자로만 된 65자 X-Request-Id 헤더를 보내면 그 요청 로그의 requestId는 보낸 값도 앞 64자도 아닌 새로 만든 UUID다")
	void sixtyFiveCharacterRequestIdHeaderIsReplacedWithNewRequestId() throws Exception {
		assertThat(SIXTY_FIVE_CHAR_REQUEST_ID).hasSize(65);

		mockMvc.perform(asUser(get(PING_PATH)).header(REQUEST_ID_HEADER, SIXTY_FIVE_CHAR_REQUEST_ID))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		String loggedRequestId = fieldsOf(requestLogs.getFirst()).get("requestId");
		assertThat(loggedRequestId)
				.isNotBlank()
				.isNotEqualTo(SIXTY_FIVE_CHAR_REQUEST_ID)
				.isNotEqualTo(SIXTY_FIVE_CHAR_REQUEST_ID.substring(0, 64))
				.matches(UUID_FORMAT);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unsafeRequestIdHeaders")
	@DisplayName("허용하지 않는 문자가 든 X-Request-Id 헤더를 보내면 그 요청 로그 한 줄의 requestId는 보낸 값이 아니라 새로 만든 UUID다")
	void unsafeRequestIdHeaderIsReplacedWithNewRequestId(String unsafeRequestId) throws Exception {
		mockMvc.perform(asUser(get(PING_PATH)).header(REQUEST_ID_HEADER, unsafeRequestId))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		String loggedRequestId = fieldsOf(requestLogs.getFirst()).get("requestId");
		assertThat(loggedRequestId)
				.isNotBlank()
				.isNotEqualTo(unsafeRequestId)
				.matches(UUID_FORMAT);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("lineBreakInjectingRequestIdHeaders")
	@DisplayName("줄바꿈과 가짜 로그 필드를 넣은 X-Request-Id 헤더를 보내면 그 요청 로그는 한 줄로 남고 requestId는 새로 만든 UUID이며 넣은 가짜 값은 로그 어디에도 없다")
	void requestIdHeaderWithLineBreakDoesNotSplitOrForgeRequestLog(String injectingRequestId) throws Exception {
		mockMvc.perform(asUser(get(PING_PATH)).header(REQUEST_ID_HEADER, injectingRequestId))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		ILoggingEvent requestLog = requestLogs.getFirst();
		assertThat(requestLog.getFormattedMessage()).doesNotContain("\n", "\r");
		Map<String, String> fields = fieldsOf(requestLog);
		assertThat(fields.get("requestId")).isNotBlank().matches(UUID_FORMAT);
		assertThat(fields).containsEntry("principal", "USER:101");
		assertThat(capturedEvents())
				.extracting(ILoggingEvent::getFormattedMessage)
				.noneMatch(message -> String.valueOf(message).contains(FORGED_REQUEST_ID))
				.noneMatch(message -> String.valueOf(message).contains(FORGED_PRINCIPAL));
	}

	@Test
	@DisplayName("@ResponseStatus(CREATED)를 단 컨트롤러 메서드 POST /test/logging/annotated-created가 ResponseEntity가 아닌 record를 돌려주면 응답은 201이고 그 요청 로그 한 줄의 status도 200이 아니라 201이다")
	void responseStatusCreatedAnnotatedMethodLogsStatus201() throws Exception {
		mockMvc.perform(asUser(post(ANNOTATED_CREATED_PATH)))
				.andExpect(status().isCreated());

		List<ILoggingEvent> requestLogs = requestLogsFor(ANNOTATED_CREATED_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst()))
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "POST")
				.containsEntry("path", ANNOTATED_CREATED_PATH)
				.containsEntry("status", "201");
	}

	@Test
	@DisplayName("@PreAuthorize(\"hasRole('GUARDIAN')\")를 단 GET /test/logging/guardian-only를 CHILD 토큰으로 불러 권한 거절로 403이 나면 응답은 403 FORBIDDEN 공통 오류 본문이고 그 요청 로그 한 줄은 status=200이 아니라 status=403 · errorCode=FORBIDDEN이다")
	void preAuthorizeDeniedRequestLogsStatus403AndForbiddenErrorCode() throws Exception {
		String childToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.CHILD);

		mockMvc.perform(get(GUARDIAN_ONLY_PATH).header(HttpHeaders.AUTHORIZATION, bearer(childToken)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, FORBIDDEN_CODE));

		List<ILoggingEvent> requestLogs = requestLogsFor(GUARDIAN_ONLY_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(requestLogs.getFirst().getFormattedMessage()).doesNotContain("\n", "\r");
		Map<String, String> fields = fieldsOf(requestLogs.getFirst());
		assertThat(fields.get("requestId")).isNotBlank();
		assertThat(fields)
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "GET")
				.containsEntry("path", GUARDIAN_ONLY_PATH)
				.containsEntry("status", "403")
				.containsEntry(ERROR_CODE_KEY, FORBIDDEN_CODE);
		assertThat(fields.get("durationMs")).isNotNull().matches("\\d+");
	}

	@Test
	@DisplayName("전화번호 · 위치 · 토큰 · 연결 코드를 담은 본문으로 POST /test/logging/sensitive-body가 200으로 끝나면 그 요청 동안 남은 어떤 로그에도 그 값들이 없다")
	void sensitiveBodyValuesAreNotLoggedWhenRequestSucceeds() throws Exception {
		mockMvc.perform(asUser(post(SENSITIVE_BODY_PATH))
						.contentType(MediaType.APPLICATION_JSON)
						.content(SENSITIVE_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone").value(SENSITIVE_PHONE))
				.andExpect(jsonPath("$.linkCode").value(SENSITIVE_LINK_CODE));

		assertThat(requestLogsFor(SENSITIVE_BODY_PATH)).hasSize(1);
		assertNoCapturedLogContains(SENSITIVE_BODY_VALUES);
	}

	@Test
	@DisplayName("같은 본문이 @Valid 검증에 걸려 POST /test/logging/strict-sensitive-body가 422 VALIDATION_FAILED로 끝나도 그 요청 동안 남은 어떤 로그에도 전화번호 · 위치 · 토큰 · 연결 코드 값이 없다")
	void sensitiveBodyValuesAreNotLoggedWhenValidationFails() throws Exception {
		mockMvc.perform(asUser(post(STRICT_SENSITIVE_BODY_PATH))
						.contentType(MediaType.APPLICATION_JSON)
						.content(SENSITIVE_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItems("phone", "lat", "lng", "token", "linkCode")));

		List<ILoggingEvent> requestLogs = requestLogsFor(STRICT_SENSITIVE_BODY_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst())).containsEntry(ERROR_CODE_KEY, "VALIDATION_FAILED");
		assertNoCapturedLogContains(SENSITIVE_BODY_VALUES);
	}

	@Test
	@DisplayName("쿼리 문자열 ?phone=01012345678&code=LINK-9F3K2를 붙여 GET /test/logging/sensitive-query를 부르면 그 요청 로그의 path는 쿼리 없는 경로이고 어떤 로그에도 쿼리 값이 없다")
	void queryStringSensitiveValuesAreNotLogged() throws Exception {
		mockMvc.perform(asUser(get(SENSITIVE_QUERY_PATH))
						.queryParam("phone", QUERY_PHONE)
						.queryParam("code", QUERY_LINK_CODE))
				.andExpect(status().isOk())
				.andExpect(content().string(QUERY_PHONE + "/" + QUERY_LINK_CODE));

		List<ILoggingEvent> requestLogs = requestLogsFor(SENSITIVE_QUERY_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst())).containsEntry("path", SENSITIVE_QUERY_PATH);
		assertThat(requestLogs.getFirst().getFormattedMessage()).doesNotContain("?", "phone=", "code=");
		assertNoCapturedLogContains(QUERY_PHONE, QUERY_LINK_CODE);
	}

	@Test
	@DisplayName("Authorization 헤더에 USER access 토큰을 담아 GET /test/logging/ping을 부르면 그 요청 동안 남은 어떤 로그에도 토큰 전체 · payload · 서명 값이 없다")
	void authorizationHeaderTokenIsNotLogged() throws Exception {
		String userToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);
		String[] tokenParts = userToken.split("\\.");
		assertThat(tokenParts).hasSize(3);

		mockMvc.perform(get(PING_PATH).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
				.andExpect(status().isOk());

		List<ILoggingEvent> requestLogs = requestLogsFor(PING_PATH);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst())).containsEntry("principal", "USER:101");
		assertNoCapturedLogContains(userToken, tokenParts[1], tokenParts[2]);
	}

	@Test
	@DisplayName("경로 변수를 가진 GET /test/logging/invites/{token}을 실제 초대 토큰 inv-7Hq2-secret으로 부르면 그 요청 로그 한 줄의 path는 실제 경로가 아니라 매핑 패턴 /test/logging/invites/{token}이고 그 요청 동안 남은 어떤 로그에도 토큰 값이 없다")
	void pathVariableRequestLogsMappingPatternWithoutActualTokenValue() throws Exception {
		mockMvc.perform(asUser(get(INVITE_PATH_PATTERN, INVITE_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value(INVITE_TOKEN));

		List<ILoggingEvent> requestLogs = requestLogsFor(INVITE_PATH_PATTERN);
		assertThat(requestLogs).hasSize(1);
		ILoggingEvent requestLog = requestLogs.getFirst();
		assertThat(requestLog.getLevel()).isEqualTo(Level.INFO);
		Map<String, String> fields = fieldsOf(requestLog);
		assertThat(fields.get("requestId")).isNotBlank();
		assertThat(fields)
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "GET")
				.containsEntry("path", INVITE_PATH_PATTERN)
				.containsEntry("status", "200");
		assertThat(fields.get("durationMs")).isNotNull().matches("\\d+");
		assertNoCapturedLogContains(INVITE_TOKEN);
	}

	@Test
	@DisplayName("경로 변수를 가진 POST /test/logging/vehicle-links/{linkCode}를 실제 연결 코드 LINK-7Q2W9로 불러 404 LINK_CODE_INVALID로 끝나도 그 요청 로그 한 줄의 path는 매핑 패턴이고 errorCode=LINK_CODE_INVALID이며 어떤 로그에도 연결 코드 값이 없다")
	void pathVariableRequestEndingWithBusinessExceptionLogsMappingPatternWithoutActualLinkCode() throws Exception {
		mockMvc.perform(asUser(post(VEHICLE_LINK_PATH_PATTERN, PATH_LINK_CODE)))
				.andExpect(commonError(HttpStatus.NOT_FOUND, LINK_CODE_INVALID));

		List<ILoggingEvent> requestLogs = requestLogsFor(VEHICLE_LINK_PATH_PATTERN);
		assertThat(requestLogs).hasSize(1);
		assertThat(fieldsOf(requestLogs.getFirst()))
				.containsEntry("principal", "USER:101")
				.containsEntry("method", "POST")
				.containsEntry("path", VEHICLE_LINK_PATH_PATTERN)
				.containsEntry("status", "404")
				.containsEntry(ERROR_CODE_KEY, LINK_CODE_INVALID);
		assertNoCapturedLogContains(PATH_LINK_CODE);
	}

	@Test
	@DisplayName("VEHICLE 77 토큰으로 SSE 스트림 GET /test/logging/stream에 연결하면 연결 때 stream=open 한 줄이 남고 이벤트 3개를 보내는 동안 줄이 늘지 않으며 스트림을 닫으면 stream=closed 한 줄이 더해져 그 경로의 요청 로그는 정확히 두 줄이다")
	void sseStreamLogsOneLineAtConnectAndOneLineAtCloseOnly() throws Exception {
		MvcResult connection = connectToStreamAsVehicle(get(STREAM_PATH));

		List<ILoggingEvent> logsAfterConnect = requestLogsFor(STREAM_PATH);
		assertThat(logsAfterConnect).hasSize(1);
		assertThat(fieldsOf(logsAfterConnect.getFirst())).containsEntry(STREAM_KEY, STREAM_OPEN);

		SseEmitter emitter = openedEmitter();
		sendStreamEvents(emitter);
		assertThat(requestLogsFor(STREAM_PATH)).hasSize(1);

		closeStream(emitter, connection);

		List<ILoggingEvent> streamLogs = requestLogsFor(STREAM_PATH);
		assertThat(streamLogs).hasSize(2);
		assertThat(fieldsOf(streamLogs.get(0))).containsEntry(STREAM_KEY, STREAM_OPEN);
		assertThat(fieldsOf(streamLogs.get(1))).containsEntry(STREAM_KEY, STREAM_CLOSED);
		assertThat(streamLogs)
				.extracting(ILoggingEvent::getFormattedMessage)
				.allSatisfy(message -> assertThat(message).doesNotContain("\n", "\r"));
	}

	@Test
	@DisplayName("VEHICLE 77 토큰으로 SSE 스트림 GET /test/logging/stream을 60ms 동안 열었다 닫으면 연결 줄과 종료 줄이 같은 requestId와 principal=VEHICLE:77 · method=GET · status=200을 담고 종료 줄의 durationMs는 연결을 유지한 시간이라 50 이상이다")
	void sseStreamConnectAndCloseLinesShareRequestIdAndVehiclePrincipal() throws Exception {
		MvcResult connection = connectToStreamAsVehicle(get(STREAM_PATH));
		SseEmitter emitter = openedEmitter();
		sendStreamEvents(emitter);
		Thread.sleep(STREAM_HOLD_MILLIS);
		closeStream(emitter, connection);

		List<ILoggingEvent> streamLogs = requestLogsFor(STREAM_PATH);
		assertThat(streamLogs).hasSize(2);
		Map<String, String> connectFields = fieldsOf(streamLogs.get(0));
		Map<String, String> closeFields = fieldsOf(streamLogs.get(1));
		assertThat(connectFields.get("requestId")).isNotBlank();
		assertThat(closeFields.get("requestId")).isEqualTo(connectFields.get("requestId"));
		for (Map<String, String> fields : List.of(connectFields, closeFields)) {
			assertThat(fields)
					.containsEntry("principal", "VEHICLE:77")
					.containsEntry("method", "GET")
					.containsEntry("path", STREAM_PATH)
					.containsEntry("status", "200");
		}
		String streamDurationMs = closeFields.get("durationMs");
		assertThat(streamDurationMs).isNotNull().matches("\\d+");
		assertThat(Long.parseLong(streamDurationMs)).isGreaterThanOrEqualTo(MIN_LOGGED_STREAM_DURATION_MILLIS);
	}

	@Test
	@DisplayName("경로 변수를 가진 SSE 스트림 GET /test/logging/streams/{streamToken}에 실제 값 stream-K3x9-secret으로 연결했다 닫으면 stream=open 줄과 stream=closed 줄의 path가 모두 매핑 패턴 /test/logging/streams/{streamToken}이고 어떤 로그에도 그 값이 없다")
	void sseStreamWithPathVariableLogsMappingPatternOnOpenAndCloseLines() throws Exception {
		MvcResult connection = connectToStreamAsVehicle(get(TOKEN_STREAM_PATH_PATTERN, STREAM_TOKEN));
		SseEmitter emitter = openedEmitter();
		sendStreamEvents(emitter);
		closeStream(emitter, connection);

		List<ILoggingEvent> streamLogs = requestLogsFor(TOKEN_STREAM_PATH_PATTERN);
		assertThat(streamLogs).hasSize(2);
		assertThat(fieldsOf(streamLogs.get(0)))
				.containsEntry(STREAM_KEY, STREAM_OPEN)
				.containsEntry("principal", "VEHICLE:77")
				.containsEntry("path", TOKEN_STREAM_PATH_PATTERN);
		assertThat(fieldsOf(streamLogs.get(1)))
				.containsEntry(STREAM_KEY, STREAM_CLOSED)
				.containsEntry("principal", "VEHICLE:77")
				.containsEntry("path", TOKEN_STREAM_PATH_PATTERN);
		assertNoCapturedLogContains(STREAM_TOKEN);
	}

	@Test
	@DisplayName("@PollingLog를 단 폴링 API GET /test/logging/link-codes/9001/status가 응답 본문 status를 WAITING · WAITING · WAITING · LINKED · LINKED 순으로 돌려주도록 VEHICLE 77이 다섯 번 부르면 codeId 9001의 요청 로그는 처음 WAITING 응답(1번째)과 처음 LINKED 응답(4번째) 두 줄만 남고 두 줄의 path는 매핑 패턴 /test/logging/link-codes/{codeId}/status다")
	void pollingEndpointLogsOnlyWhenResponseBodyStatusDiffersFromPreviousPoll() throws Exception {
		List<String> responseStatuses = List.of(WAITING, WAITING, WAITING, LINKED, LINKED);
		linkCodeStatuses.script(CHANGING_CODE_ID, responseStatuses);

		for (int call = 1; call <= responseStatuses.size(); call++) {
			pollAsVehicle(CHANGING_CODE_ID, call)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value(responseStatuses.get(call - 1)));
		}

		List<ILoggingEvent> pollingLogs = pollingLogsFor(CHANGING_CODE_ID);
		assertThat(fieldValuesOf(pollingLogs, "requestId"))
				.containsExactly(pollRequestIdOf(CHANGING_CODE_ID, 1), pollRequestIdOf(CHANGING_CODE_ID, 4));
		assertThat(pollingLogs).allSatisfy(pollingLog -> {
			assertThat(pollingLog.getLevel()).isEqualTo(Level.INFO);
			assertThat(fieldsOf(pollingLog))
					.containsEntry("principal", "VEHICLE:77")
					.containsEntry("method", "GET")
					.containsEntry("path", POLLING_PATH_TEMPLATE)
					.containsEntry("status", "200")
					.doesNotContainKey(ERROR_CODE_KEY);
			assertThat(fieldsOf(pollingLog).get("durationMs")).isNotNull().matches("\\d+");
		});
	}

	@Test
	@DisplayName("폴링 API를 codeId 9101(WAITING · WAITING · LINKED)과 9102(WAITING · WAITING)로 번갈아 부르면 직전 status를 codeId마다 따로 기억해 9101은 1번째 WAITING과 3번째 LINKED 두 줄이 남고 9102는 9101과 같은 WAITING이어도 1번째 줄이 남는다")
	void pollingEndpointComparesWithPreviousStatusOfSameCodeIdOnly() throws Exception {
		linkCodeStatuses.script(FIRST_INTERLEAVED_CODE_ID, List.of(WAITING, WAITING, LINKED));
		linkCodeStatuses.script(SECOND_INTERLEAVED_CODE_ID, List.of(WAITING, WAITING));

		pollAsVehicle(FIRST_INTERLEAVED_CODE_ID, 1).andExpect(jsonPath("$.status").value(WAITING));
		pollAsVehicle(SECOND_INTERLEAVED_CODE_ID, 1).andExpect(jsonPath("$.status").value(WAITING));
		pollAsVehicle(FIRST_INTERLEAVED_CODE_ID, 2).andExpect(jsonPath("$.status").value(WAITING));
		pollAsVehicle(SECOND_INTERLEAVED_CODE_ID, 2).andExpect(jsonPath("$.status").value(WAITING));
		pollAsVehicle(FIRST_INTERLEAVED_CODE_ID, 3).andExpect(jsonPath("$.status").value(LINKED));

		assertThat(fieldValuesOf(pollingLogsFor(FIRST_INTERLEAVED_CODE_ID), "requestId"))
				.containsExactly(
						pollRequestIdOf(FIRST_INTERLEAVED_CODE_ID, 1),
						pollRequestIdOf(FIRST_INTERLEAVED_CODE_ID, 3));
		assertThat(fieldValuesOf(pollingLogsFor(SECOND_INTERLEAVED_CODE_ID), "requestId"))
				.containsExactly(pollRequestIdOf(SECOND_INTERLEAVED_CODE_ID, 1));
	}

	@Test
	@DisplayName("폴링 API codeId 9201이 WAITING · WAITING 200 다음에 404 LINK_CODE_INVALID를 두 번 연달아 돌려주면 같은 WAITING 두 번째 줄은 빠지고 오류 응답은 직전과 같아도 매번 남아 1번째 · 3번째 · 4번째 세 줄이 남고 404 두 줄에 errorCode=LINK_CODE_INVALID가 들어 있다")
	void pollingEndpointAlwaysLogsErrorResponsesEvenWhenRepeated() throws Exception {
		linkCodeStatuses.script(INVALIDATED_CODE_ID, List.of(WAITING, WAITING, LINK_CODE_INVALID, LINK_CODE_INVALID));

		pollAsVehicle(INVALIDATED_CODE_ID, 1).andExpect(status().isOk());
		pollAsVehicle(INVALIDATED_CODE_ID, 2).andExpect(status().isOk());
		pollAsVehicle(INVALIDATED_CODE_ID, 3).andExpect(commonError(HttpStatus.NOT_FOUND, LINK_CODE_INVALID));
		pollAsVehicle(INVALIDATED_CODE_ID, 4).andExpect(commonError(HttpStatus.NOT_FOUND, LINK_CODE_INVALID));

		List<ILoggingEvent> pollingLogs = pollingLogsFor(INVALIDATED_CODE_ID);
		assertThat(fieldValuesOf(pollingLogs, "requestId"))
				.containsExactly(
						pollRequestIdOf(INVALIDATED_CODE_ID, 1),
						pollRequestIdOf(INVALIDATED_CODE_ID, 3),
						pollRequestIdOf(INVALIDATED_CODE_ID, 4));
		assertThat(fieldValuesOf(pollingLogs, "status")).containsExactly("200", "404", "404");
		assertThat(fieldValuesOf(pollingLogs, ERROR_CODE_KEY))
				.containsExactly(null, LINK_CODE_INVALID, LINK_CODE_INVALID);
	}

	static Stream<Arguments> unsafeRequestIdHeaders() {
		return Stream.of(
				arguments(named("공백이 든 값", "req 123")),
				arguments(named("등호가 든 값", "req=123")),
				arguments(named("세미콜론이 든 값", "req;123")),
				arguments(named("한글이 든 값", "요청123")),
				arguments(named("빈 값", "")));
	}

	static Stream<Arguments> lineBreakInjectingRequestIdHeaders() {
		String forgedFields = "requestId=" + FORGED_REQUEST_ID + " principal=" + FORGED_PRINCIPAL;
		return Stream.of(
				arguments(named("LF 줄바꿈", "req-1\n" + forgedFields)),
				arguments(named("CRLF 줄바꿈", "req-1\r\n" + forgedFields)));
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
		String userToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);
		return request.header(HttpHeaders.AUTHORIZATION, bearer(userToken));
	}

	private MvcResult connectToStreamAsVehicle(MockHttpServletRequestBuilder streamRequest) throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);
		return mockMvc.perform(streamRequest
						.header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken))
						.accept(MediaType.TEXT_EVENT_STREAM))
				.andExpect(request().asyncStarted())
				.andReturn();
	}

	private ResultActions pollAsVehicle(long codeId, int call) throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);
		return mockMvc.perform(get(POLLING_PATH_TEMPLATE, codeId)
				.header(HttpHeaders.AUTHORIZATION, bearer(vehicleToken))
				.header(REQUEST_ID_HEADER, pollRequestIdOf(codeId, call)));
	}

	private List<ILoggingEvent> pollingLogsFor(long codeId) {
		String requestIdPrefix = pollRequestIdPrefixOf(codeId);
		return requestLogsFor(POLLING_PATH_TEMPLATE).stream()
				.filter(event -> String.valueOf(fieldsOf(event).get("requestId")).startsWith(requestIdPrefix))
				.toList();
	}

	private static String pollRequestIdOf(long codeId, int call) {
		return pollRequestIdPrefixOf(codeId) + call;
	}

	private static String pollRequestIdPrefixOf(long codeId) {
		return "poll-" + codeId + "-";
	}

	private static List<String> fieldValuesOf(List<ILoggingEvent> events, String key) {
		return events.stream()
				.map(event -> fieldsOf(event).get(key))
				.toList();
	}

	private SseEmitter openedEmitter() {
		SseEmitter emitter = sseConnections.latest();
		assertThat(emitter).isNotNull();
		return emitter;
	}

	private static void sendStreamEvents(SseEmitter emitter) throws IOException {
		for (int seq = 1; seq <= STREAM_EVENT_COUNT; seq++) {
			emitter.send(SseEmitter.event().name(STREAM_EVENT_NAME).id(String.valueOf(seq)).data("seq-" + seq));
		}
	}

	private void closeStream(SseEmitter emitter, MvcResult connection) throws Exception {
		emitter.complete();
		mockMvc.perform(asyncDispatch(connection))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("event:" + STREAM_EVENT_NAME)))
				.andExpect(content().string(containsString("data:seq-" + STREAM_EVENT_COUNT)));
	}

	private List<ILoggingEvent> requestLogsFor(String path) {
		return capturedEvents().stream()
				.filter(event -> path.equals(fieldsOf(event).get("path")))
				.toList();
	}

	private List<ILoggingEvent> capturedEvents() {
		synchronized (logCapture) {
			return new ArrayList<>(logCapture.list);
		}
	}

	private void assertNoCapturedLogContains(String... sensitiveValues) {
		List<String> texts = capturedEvents().stream()
				.flatMap(event -> textsOf(event).stream())
				.toList();
		assertThat(texts).isNotEmpty();
		for (String sensitiveValue : sensitiveValues) {
			assertThat(texts)
					.as("로그에 민감값 %s가 그대로 남으면 안 된다", sensitiveValue)
					.noneMatch(text -> text.contains(sensitiveValue));
		}
	}

	private static List<String> textsOf(ILoggingEvent event) {
		List<String> texts = new ArrayList<>();
		texts.add(String.valueOf(event.getFormattedMessage()));
		texts.addAll(event.getMDCPropertyMap().values());
		collectThrowableTexts(event.getThrowableProxy(), texts);
		return texts;
	}

	private static void collectThrowableTexts(IThrowableProxy throwable, List<String> texts) {
		if (throwable == null) {
			return;
		}
		texts.add(throwable.getClassName() + ": " + throwable.getMessage());
		for (IThrowableProxy suppressed : throwable.getSuppressed()) {
			collectThrowableTexts(suppressed, texts);
		}
		collectThrowableTexts(throwable.getCause(), texts);
	}

	private static Map<String, String> fieldsOf(ILoggingEvent event) {
		Map<String, String> fields = new HashMap<>();
		Matcher matcher = LOG_FIELD.matcher(String.valueOf(event.getFormattedMessage()));
		while (matcher.find()) {
			fields.putIfAbsent(matcher.group(1), matcher.group(2));
		}
		return fields;
	}

	private static Logger rootLogger() {
		return (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
	}

	@RestController
	static class RequestLoggingTestController {

		@GetMapping(PING_PATH)
		public String ping() {
			return "pong";
		}

		@GetMapping(SLOW_PATH)
		public String slow() throws InterruptedException {
			Thread.sleep(SLOW_HANDLER_SLEEP_MILLIS);
			return "slow";
		}

		@PostMapping(ITEMS_PATH)
		public ResponseEntity<String> create() {
			return ResponseEntity.status(HttpStatus.CREATED).body("created");
		}

		@GetMapping(PUBLIC_PING_PATH)
		public String publicPing() {
			return "public-pong";
		}

		@GetMapping(BUSINESS_ERROR_PATH)
		public String businessError() {
			throw BusinessException.of(HttpStatus.CONFLICT, DOMAIN_ERROR_CODE, DOMAIN_ERROR_MESSAGE);
		}

		@GetMapping(UNEXPECTED_ERROR_PATH)
		public String unexpectedError() {
			throw new IllegalStateException(INTERNAL_DETAIL);
		}

		@PostMapping(VALIDATED_PATH)
		public ValidatedLoggingRequest validated(@Valid @RequestBody ValidatedLoggingRequest request) {
			return request;
		}

		@PostMapping(ANNOTATED_CREATED_PATH)
		@ResponseStatus(HttpStatus.CREATED)
		public CreatedLoggingResponse createWithAnnotatedStatus() {
			return new CreatedLoggingResponse(CREATED_ITEM_ID);
		}

		@GetMapping(GUARDIAN_ONLY_PATH)
		@PreAuthorize("hasRole('GUARDIAN')")
		public String guardianOnly() {
			return "guardian-ok";
		}

		@PostMapping(SENSITIVE_BODY_PATH)
		public SensitiveLoggingRequest sensitiveBody(@Valid @RequestBody SensitiveLoggingRequest request) {
			return request;
		}

		@PostMapping(STRICT_SENSITIVE_BODY_PATH)
		public StrictSensitiveLoggingRequest strictSensitiveBody(
				@Valid @RequestBody StrictSensitiveLoggingRequest request) {
			return request;
		}

		@GetMapping(SENSITIVE_QUERY_PATH)
		public String sensitiveQuery(@RequestParam("phone") String phone, @RequestParam("code") String code) {
			return phone + "/" + code;
		}

		@GetMapping(INVITE_PATH_PATTERN)
		public InviteLookupResponse invite(@PathVariable("token") String token) {
			return new InviteLookupResponse(token);
		}

		@PostMapping(VEHICLE_LINK_PATH_PATTERN)
		public String linkVehicle(@PathVariable("linkCode") String linkCode) {
			throw BusinessException.of(HttpStatus.NOT_FOUND, LINK_CODE_INVALID, LINK_CODE_INVALID_MESSAGE);
		}

	}

	@RestController
	@RequiredArgsConstructor
	static class RequestLoggingSseTestController {

		private final TestSseConnections connections;

		@GetMapping(path = STREAM_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		public SseEmitter stream() {
			return connections.open();
		}

		@GetMapping(path = TOKEN_STREAM_PATH_PATTERN, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		public SseEmitter streamByToken(@PathVariable("streamToken") String streamToken) {
			return connections.open();
		}

	}

	@RestController
	@RequiredArgsConstructor
	static class RequestLoggingPollingTestController {

		private final ScriptedLinkCodeStatuses linkCodeStatuses;

		@GetMapping(POLLING_PATH_TEMPLATE)
		@PollingLog(stateField = POLL_STATE_FIELD)
		public PollingStatusResponse linkCodeStatus(@PathVariable("codeId") long codeId) {
			String outcome = linkCodeStatuses.next(codeId);
			if (LINK_CODE_INVALID.equals(outcome)) {
				throw BusinessException.of(HttpStatus.NOT_FOUND, LINK_CODE_INVALID, LINK_CODE_INVALID_MESSAGE);
			}
			return PollingStatusResponse.of(PollingLinkStatus.valueOf(outcome));
		}

	}

	static class ScriptedLinkCodeStatuses {

		private final Map<Long, Deque<String>> outcomesByCodeId = new ConcurrentHashMap<>();

		void script(long codeId, List<String> outcomes) {
			outcomesByCodeId.put(codeId, new ConcurrentLinkedDeque<>(outcomes));
		}

		String next(long codeId) {
			Deque<String> outcomes = outcomesByCodeId.get(codeId);
			if (outcomes == null || outcomes.isEmpty()) {
				throw new IllegalStateException("codeId " + codeId + "의 응답 스크립트가 없습니다");
			}
			return outcomes.poll();
		}

	}

	enum PollingLinkStatus {
		WAITING, LINKED
	}

	record PollingStatusResponse(PollingLinkStatus status, Integer pollAfterSeconds) {

		static PollingStatusResponse of(PollingLinkStatus status) {
			Integer pollAfterSeconds = status == PollingLinkStatus.WAITING ? WAITING_POLL_AFTER_SECONDS : null;
			return new PollingStatusResponse(status, pollAfterSeconds);
		}

	}

	static class TestSseConnections {

		private final AtomicReference<SseEmitter> latest = new AtomicReference<>();

		SseEmitter open() {
			SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
			latest.set(emitter);
			return emitter;
		}

		SseEmitter latest() {
			return latest.get();
		}

	}

	record ValidatedLoggingRequest(@NotBlank String name) {
	}

	record SensitiveLoggingRequest(
			@NotBlank String phone,
			double lat,
			double lng,
			@NotBlank String token,
			@NotBlank String linkCode) {
	}

	record StrictSensitiveLoggingRequest(
			@jakarta.validation.constraints.Pattern(regexp = "\\d{11}") String phone,
			@DecimalMax("30.0") double lat,
			@DecimalMax("30.0") double lng,
			@Size(max = 8) String token,
			@jakarta.validation.constraints.Pattern(regexp = "\\d{6}") String linkCode) {
	}

	record CreatedLoggingResponse(long id) {
	}

	record InviteLookupResponse(String token) {
	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import({RequestLoggingTestController.class, RequestLoggingSseTestController.class, TestSseConnections.class,
			RequestLoggingPollingTestController.class, ScriptedLinkCodeStatuses.class})
	static class RequestLoggingTestEndpointConfiguration {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain publicRequestLoggingSecurityFilterChain(HttpSecurity http) throws Exception {
			http.securityMatcher(PUBLIC_ENDPOINT_PATTERN)
					.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
					.csrf(AbstractHttpConfigurer::disable);
			return http.build();
		}

	}

}

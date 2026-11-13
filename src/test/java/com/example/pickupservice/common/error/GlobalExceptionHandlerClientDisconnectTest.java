package com.example.pickupservice.common.error;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;
import java.util.List;

import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerClientDisconnectTest {

	private static final String ASYNC_NOT_USABLE_PATH = "/disconnect/async-request-not-usable";
	private static final String BROKEN_PIPE_PATH = "/disconnect/broken-pipe";
	private static final String UNHANDLED_PATH = "/disconnect/unhandled";
	private static final String BAD_GATEWAY_BUSINESS_PATH = "/disconnect/business-bad-gateway";
	private static final String CONFLICT_BUSINESS_PATH = "/disconnect/business-conflict";
	private static final String NOT_FOUND_BUSINESS_PATH = "/disconnect/business-not-found";

	private static final String MAP_API_UNAVAILABLE_CODE = "MAP_API_UNAVAILABLE";
	private static final String MAP_API_UNAVAILABLE_MESSAGE = "지도 검색을 잠시 사용할 수 없습니다";
	private static final String MAP_API_CAUSE_MESSAGE = "kakao map api read timeout";
	private static final String DOMAIN_CODE = "SOME_DOMAIN_CODE";
	private static final String DOMAIN_CODE_MESSAGE = "도메인 규칙에 맞지 않는 요청입니다";

	private Logger handlerLogger;
	private Level originalLevel;
	private ListAppender<ILoggingEvent> appender;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		originalLevel = handlerLogger.getLevel();
		handlerLogger.setLevel(Level.DEBUG);
		appender = new ListAppender<>();
		appender.start();
		handlerLogger.addAppender(appender);
		mockMvc = MockMvcBuilders.standaloneSetup(new DisconnectTestController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@AfterEach
	void tearDown() {
		handlerLogger.detachAppender(appender);
		appender.stop();
		handlerLogger.setLevel(originalLevel);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {ASYNC_NOT_USABLE_PATH, BROKEN_PIPE_PATH})
	@DisplayName("클라이언트가 연결을 끊어 난 예외(AsyncRequestNotUsableException · Broken pipe IOException)는 ERROR로 기록하지 않고 DEBUG로만 남기며 응답 본문을 쓰지 않는다")
	void clientDisconnectIsLoggedAtDebugOnlyAndWritesNoBody(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(result -> {
					assertThat(result.getResponse().getContentAsString()).isEmpty();
					assertThat(result.getResponse().getContentType()).satisfiesAnyOf(
							type -> assertThat(type).isNull(),
							type -> assertThat(type).doesNotContain("json"));
					assertThat(result.getRequest().getAttribute(ErrorCodeAttribute.NAME)).isNull();
				});

		assertThat(eventsAt(Level.ERROR)).isEmpty();
		assertThat(eventsAt(Level.DEBUG)).isNotEmpty();
	}

	@Test
	@DisplayName("클라이언트 끊김이 아닌 처리하지 않은 예외(IllegalStateException)는 ERROR로 기록하고 500 INTERNAL_SERVER_ERROR 본문을 돌려준다")
	void unhandledExceptionIsLoggedAtErrorAndReturns500Body() throws Exception {
		mockMvc.perform(get(UNHANDLED_PATH))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));

		List<ILoggingEvent> errors = eventsAt(Level.ERROR);
		assertThat(errors).hasSize(1);
		assertThat(errors.get(0).getThrowableProxy()).isNotNull();
		assertThat(errors.get(0).getThrowableProxy().getMessage()).isEqualTo("boom");
	}

	@Test
	@DisplayName("5xx로 끝나는 BusinessException(502 MAP_API_UNAVAILABLE · 원인 예외 연결)은 원인 예외 스택과 함께 ERROR 로그를 1건 남기고 502 공통 오류 본문(code · message · retryAfterSeconds null)을 돌려준다")
	void serverErrorBusinessExceptionIsLoggedAtErrorWithCauseAndReturnsCommonErrorBody() throws Exception {
		mockMvc.perform(get(BAD_GATEWAY_BUSINESS_PATH))
				.andExpect(commonError(HttpStatus.BAD_GATEWAY, MAP_API_UNAVAILABLE_CODE))
				.andExpect(jsonPath("$.error.message").value(MAP_API_UNAVAILABLE_MESSAGE));

		List<ILoggingEvent> errors = eventsAt(Level.ERROR);
		assertThat(errors).hasSize(1);
		assertThat(errors.get(0).getThrowableProxy()).isNotNull();
		assertThat(errors.get(0).getThrowableProxy().getClassName()).isEqualTo(BusinessException.class.getName());
		assertThat(errors.get(0).getThrowableProxy().getCause()).isNotNull();
		assertThat(errors.get(0).getThrowableProxy().getCause().getClassName())
				.isEqualTo(IllegalStateException.class.getName());
		assertThat(errors.get(0).getThrowableProxy().getCause().getMessage()).isEqualTo(MAP_API_CAUSE_MESSAGE);
		assertThat(errors.get(0).getThrowableProxy().getCause().getStackTraceElementProxyArray()).isNotEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {CONFLICT_BUSINESS_PATH, NOT_FOUND_BUSINESS_PATH})
	@DisplayName("4xx로 끝나는 BusinessException(409 SOME_DOMAIN_CODE · 404 NOT_FOUND)은 지금처럼 ERROR 로그를 남기지 않고 공통 오류 본문을 돌려준다")
	void clientErrorBusinessExceptionIsNotLoggedAtError(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(result -> assertThat(result.getResponse().getStatus()).isBetween(400, 499))
				.andExpect(jsonPath("$.error.code").isNotEmpty());

		assertThat(eventsAt(Level.ERROR)).isEmpty();
	}

	private List<ILoggingEvent> eventsAt(Level level) {
		return appender.list.stream()
				.filter(event -> event.getLevel() == level)
				.toList();
	}

	@RestController
	static class DisconnectTestController {

		@GetMapping(ASYNC_NOT_USABLE_PATH)
		public String throwAsyncRequestNotUsable() throws Exception {
			throw new AsyncRequestNotUsableException("Servlet container error notification for disconnected client",
					new IOException("Broken pipe"));
		}

		@GetMapping(BROKEN_PIPE_PATH)
		public String throwBrokenPipe() throws Exception {
			throw new IOException("Broken pipe");
		}

		@GetMapping(UNHANDLED_PATH)
		public String throwUnhandled() {
			throw new IllegalStateException("boom");
		}

		@GetMapping(BAD_GATEWAY_BUSINESS_PATH)
		public String throwBadGatewayBusiness() {
			BusinessException exception = BusinessException.of(HttpStatus.BAD_GATEWAY, MAP_API_UNAVAILABLE_CODE,
					MAP_API_UNAVAILABLE_MESSAGE);
			exception.initCause(new IllegalStateException(MAP_API_CAUSE_MESSAGE));
			throw exception;
		}

		@GetMapping(CONFLICT_BUSINESS_PATH)
		public String throwConflictBusiness() {
			throw BusinessException.of(HttpStatus.CONFLICT, DOMAIN_CODE, DOMAIN_CODE_MESSAGE);
		}

		@GetMapping(NOT_FOUND_BUSINESS_PATH)
		public String throwNotFoundBusiness() {
			throw BusinessException.of(ErrorCode.NOT_FOUND);
		}

	}

}

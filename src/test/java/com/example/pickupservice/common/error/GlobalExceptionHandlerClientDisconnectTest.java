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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerClientDisconnectTest {

	private static final String ASYNC_NOT_USABLE_PATH = "/disconnect/async-request-not-usable";
	private static final String BROKEN_PIPE_PATH = "/disconnect/broken-pipe";
	private static final String UNHANDLED_PATH = "/disconnect/unhandled";

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

	}

}

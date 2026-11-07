package com.example.pickupservice.auth.adapter.in.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.logging.RequestLoggingInterceptor;
import com.example.pickupservice.common.security.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@RequiredArgsConstructor
class PreLinkTokenWebTest extends AuthWebTestSupport {

	private static final String PUSH_TOKEN_PATH = "/v1/devices/me/push-token";
	private static final String SESSION_PATH = "/v1/vehicle-devices/me/session";
	private static final long VEHICLE_ID = 88L;
	private static final long DEVICE_ID = 388L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	private ListAppender<ILoggingEvent> logCapture;

	@BeforeEach
	void attachLogCapture() {
		logCapture = new ListAppender<>();
		logCapture.start();
		requestLogger().addAppender(logCapture);
	}

	@AfterEach
	void detachLogCapture() {
		requestLogger().detachAppender(logCapture);
		logCapture.stop();
	}

	@Test
	@DisplayName("PUT /v1/devices/me/push-token을 연결 전 차량 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void updatePushTokenWithPreLinkVehicleTokenReturns403() throws Exception {
		mockMvc.perform(pushTokenRequest(preLinkVehicleToken()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(updatePushTokenUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("DELETE /v1/vehicle-devices/me/session을 연결 전 차량 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void logoutVehicleWithPreLinkVehicleTokenReturns403() throws Exception {
		mockMvc.perform(delete(SESSION_PATH).header(HttpHeaders.AUTHORIZATION, bearer(preLinkVehicleToken())))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(logoutVehicleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("연결 전 차량 토큰이 막힌 403도 요청 로그에 status=403 · errorCode=FORBIDDEN 한 줄로 남는다")
	void preLinkForbiddenIsRequestLogged() throws Exception {
		mockMvc.perform(pushTokenRequest(preLinkVehicleToken()));

		assertThat(requestLogsFor(PUSH_TOKEN_PATH))
				.singleElement()
				.satisfies(message -> assertThat(message).contains(
						"principal=VEHICLE:" + VEHICLE_ID, "status=403", "errorCode=FORBIDDEN"));
	}

	private String preLinkVehicleToken() {
		return accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(VEHICLE_ID),
				"principalType", PrincipalType.VEHICLE.name(),
				"deviceId", DEVICE_ID));
	}

	private static MockHttpServletRequestBuilder pushTokenRequest(String accessToken) {
		return put(PUSH_TOKEN_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"platform\": \"VEHICLE\", \"pushToken\": \"fcm-push-token-123\"}");
	}

	private List<String> requestLogsFor(String path) {
		return logCapture.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.filter(message -> message.contains("path=" + path + " ") || message.endsWith("path=" + path))
				.toList();
	}

	private static Logger requestLogger() {
		return (Logger) LoggerFactory.getLogger(RequestLoggingInterceptor.class);
	}

}

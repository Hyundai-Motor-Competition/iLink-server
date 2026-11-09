package com.example.pickupservice.notification.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.notification.application.port.in.DeliveryReceiptCommand;
import com.example.pickupservice.notification.application.port.in.NotificationListQuery;
import com.example.pickupservice.notification.application.port.in.NotificationView;
import com.example.pickupservice.notification.application.port.in.ResendCommand;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class NotificationControllerTest extends NotificationWebTestSupport {

	private static final String RECEIPT_PATH = "/v1/notifications/{notificationId}/delivery-receipt";
	private static final String RECEIPT_BODY = """
			{"receivedAt":"2026-11-05T18:09:04+09:00"}
			""";
	private static final Instant RECEIVED_AT = Instant.parse("2026-11-05T09:09:04Z");
	private static final String LIST_PATH = "/v1/pickup-sessions/{sessionId}/notifications";
	private static final long FAMILY_ID = 12L;
	private static final long SESSION_ID = 9001L;
	private static final long NOTIFICATION_ID = 7001L;
	private static final long NEW_NOTIFICATION_ID = 7002L;
	private static final String RESEND_PATH = "/v1/notifications/{notificationId}/resend";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final int RETRY_AFTER_SECONDS = 30;
	private static final long TEACHER_USER_ID = 141L;
	private static final long CHILD_USER_ID = 31L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final AuthPrincipal CHILD = user(CHILD_USER_ID, Role.CHILD);
	private static final AuthPrincipal GUARDIAN = user(GUARDIAN_USER_ID, Role.GUARDIAN);

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	/**
	 * NOTI-01은 알림을 받은 기기의 앱이 부른다(보호자 앱 · 자녀 앱).
	 */
	static Stream<AuthPrincipal> receivers() {
		return Stream.of(CHILD, GUARDIAN);
	}

	/**
	 * NOTI-02 권한: GUARDIAN · CO_GUARDIAN · DELEGATE.
	 */
	static Stream<AuthPrincipal> historyViewers() {
		return Stream.of(
				GUARDIAN,
				user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN),
				user(DELEGATE_USER_ID, Role.DELEGATE));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("receivers")
	@DisplayName("POST /v1/notifications/{notificationId}/delivery-receipt는 204를 돌려준다")
	void deliveryReceiptReturns204(AuthPrincipal principal) throws Exception {
		mockMvc.perform(post(RECEIPT_PATH, NOTIFICATION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
						.contentType(MediaType.APPLICATION_JSON)
						.content(RECEIPT_BODY))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		then(recordDeliveryUseCase).should().recordDelivery(
				new DeliveryReceiptCommand(principal, NOTIFICATION_ID, RECEIVED_AT));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"{}", "{\"receivedAt\":null}"})
	@DisplayName("POST /v1/notifications/{notificationId}/delivery-receipt에 receivedAt이 없으면 422 VALIDATION_FAILED를 돌려준다")
	void deliveryReceiptWithoutReceivedAtReturns422ValidationFailed(String body) throws Exception {
		mockMvc.perform(post(RECEIPT_PATH, NOTIFICATION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(CHILD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("receivedAt"));

		then(recordDeliveryUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("historyViewers")
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/notifications는 200과 items[](notificationId · type · receiverName · title · sentAt · deliveredAt · openedAt · attempt)를 돌려준다")
	void listReturns200WithItems(AuthPrincipal principal) throws Exception {
		given(listNotificationsUseCase.list(any())).willReturn(List.of(
				new NotificationView(NOTIFICATION_ID, "CALL", "민준", "이제 나와요",
						Instant.parse("2026-11-05T09:09:02Z"), Instant.parse("2026-11-05T09:09:04Z"), null, 1)));

		mockMvc.perform(get(LIST_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal)))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[{"notificationId":7001,"type":"CALL","receiverName":"민준","title":"이제 나와요",\
						"sentAt":"2026-11-05T18:09:02+09:00","deliveredAt":"2026-11-05T18:09:04+09:00",\
						"openedAt":null,"attempt":1}]}
						""", JsonCompareMode.STRICT));

		then(listNotificationsUseCase).should().list(new NotificationListQuery(principal, SESSION_ID));
	}

	@Test
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/notifications를 CHILD 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void listWithChildTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(get(LIST_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(CHILD)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(listNotificationsUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("historyViewers")
	@DisplayName("POST /v1/notifications/{notificationId}/resend는 202와 새 notificationId를 돌려준다")
	void resendReturns202WithNewNotificationId(AuthPrincipal principal) throws Exception {
		given(resendNotificationUseCase.resend(any())).willReturn(NEW_NOTIFICATION_ID);

		mockMvc.perform(post(RESEND_PATH, NOTIFICATION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(status().isAccepted())
				.andExpect(content().json("""
						{"notificationId":7002}
						""", JsonCompareMode.STRICT));

		then(resendNotificationUseCase).should().resend(new ResendCommand(principal, NOTIFICATION_ID));
	}

	@Test
	@DisplayName("POST /v1/notifications/{notificationId}/resend가 1분 안이면 429 RESEND_TOO_SOON과 retryAfterSeconds를 돌려준다")
	void resendWithinOneMinuteReturns429ResendTooSoon() throws Exception {
		given(resendNotificationUseCase.resend(any()))
				.willThrow(BusinessException.tooManyRequests(ErrorCode.RESEND_TOO_SOON, RETRY_AFTER_SECONDS));

		mockMvc.perform(post(RESEND_PATH, NOTIFICATION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(GUARDIAN))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(status().isTooManyRequests())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.error.code").value("RESEND_TOO_SOON"))
				.andExpect(jsonPath("$.error.message").value("잠시 뒤에 다시 보내 주세요"))
				.andExpect(jsonPath("$.error.retryAfterSeconds").value(RETRY_AFTER_SECONDS))
				.andExpect(jsonPath("$.error.fields").doesNotExist());
	}

	@Test
	@DisplayName("POST /v1/notifications/{notificationId}/resend를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void resendWithTeacherTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(post(RESEND_PATH, NOTIFICATION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(user(TEACHER_USER_ID, Role.TEACHER)))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(resendNotificationUseCase).shouldHaveNoInteractions();
	}

	private String bearerOf(AuthPrincipal principal) {
		return bearer(accessTokenFixture.user(principal.id(), principal.familyId(), principal.role()));
	}

	private static AuthPrincipal user(long userId, Role role) {
		return new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
	}

}

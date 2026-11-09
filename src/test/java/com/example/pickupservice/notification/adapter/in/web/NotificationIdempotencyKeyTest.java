package com.example.pickupservice.notification.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class NotificationIdempotencyKeyTest extends NotificationWebTestSupport {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long NOTIFICATION_ID = 7001L;
	private static final long SESSION_ID = 9001L;
	private static final String RESEND_PATH = "/v1/notifications/{notificationId}/resend";
	private static final String RECEIPT_PATH = "/v1/notifications/{notificationId}/delivery-receipt";
	private static final String LIST_PATH = "/v1/pickup-sessions/{sessionId}/notifications";
	private static final String RECEIPT_BODY = """
			{"receivedAt":"2026-11-05T18:09:04+09:00"}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("Idempotency-Key는 POST /v1/notifications/{notificationId}/resend에만 요구된다")
	void idempotencyKeyIsRequiredOnlyForResend() {
		given(listNotificationsUseCase.list(any())).willReturn(List.of());

		assertAll(
				() -> mockMvc.perform(withGuardian(post(RESEND_PATH, NOTIFICATION_ID)))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(post(RECEIPT_PATH, NOTIFICATION_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content(RECEIPT_BODY)))
						.andExpect(status().isNoContent()),
				() -> mockMvc.perform(withGuardian(get(LIST_PATH, SESSION_ID)))
						.andExpect(status().isOk()));

		then(resendNotificationUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder withGuardian(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION,
				bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN)));
	}

	private static ResultMatcher missingIdempotencyKey() {
		return result -> {
			status().isUnprocessableContent().match(result);
			commonErrorKeys().match(result);
			jsonPath("$.error.code").value("VALIDATION_FAILED").match(result);
			jsonPath("$.error.fields", hasSize(1)).match(result);
			jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER).match(result);
		};
	}

}

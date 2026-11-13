package com.example.pickupservice.common.fixture;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ErrorResponseMatchers {

	private ErrorResponseMatchers() {
	}

	public static ResultMatcher commonError(HttpStatus expectedStatus, String expectedCode) {
		return result -> {
			status().is(expectedStatus.value()).match(result);
			commonErrorKeys().match(result);
			jsonPath("$.error.code").value(expectedCode).match(result);
			jsonPath("$.error.fields").doesNotExist().match(result);
		};
	}

	// 429 응답: code · message · retryAfterSeconds 키가 있고 retryAfterSeconds는 다시 시도할 초(null 아님), fields는 없다
	public static ResultMatcher tooManyRequestsError(String expectedCode, int expectedRetryAfterSeconds) {
		return result -> {
			status().isTooManyRequests().match(result);
			jsonPath("$.error", allOf(hasKey("code"), hasKey("message"), hasKey("retryAfterSeconds"))).match(result);
			content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
			jsonPath("$.error.code").value(expectedCode).match(result);
			jsonPath("$.error.message").isNotEmpty().match(result);
			jsonPath("$.error.retryAfterSeconds").value(expectedRetryAfterSeconds).match(result);
			jsonPath("$.error.fields").doesNotExist().match(result);
		};
	}

	public static ResultMatcher commonErrorKeys() {
		return result -> {
			jsonPath("$.error", allOf(hasKey("code"), hasKey("message"), hasKey("retryAfterSeconds"))).match(result);
			content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(result);
			jsonPath("$.error.code").isNotEmpty().match(result);
			jsonPath("$.error.message").isNotEmpty().match(result);
			jsonPath("$.error.retryAfterSeconds").value(nullValue()).match(result);
		};
	}

}

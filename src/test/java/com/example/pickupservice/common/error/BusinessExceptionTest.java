package com.example.pickupservice.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class BusinessExceptionTest {

	private static final String GUARD_UNMET_CODE = "CALL_GUARD_UNMET";
	private static final String GUARD_UNMET_MESSAGE = "합류 안내를 보낼 수 있는 조건이 아닙니다";
	private static final String STATE_VERSION_CONFLICT = "STATE_VERSION_CONFLICT";

	@Test
	@DisplayName("conflictWithOverlap(409 · SCHEDULE_OVERLAP · 메시지 · [801, 802])로 만든 예외는 status 409 · code SCHEDULE_OVERLAP · 메시지 그대로 · overlapScheduleIds [801, 802] · retryAfterSeconds null · fields null이다")
	void conflictWithOverlapKeepsOverlapScheduleIds() {
		BusinessException exception = BusinessException.conflictWithOverlap(
				HttpStatus.CONFLICT, "SCHEDULE_OVERLAP", "겹치는 일정이 있습니다", List.of(801L, 802L));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SCHEDULE_OVERLAP");
		assertThat(exception.getMessage()).isEqualTo("겹치는 일정이 있습니다");
		assertThat(exception.overlapScheduleIds()).containsExactly(801L, 802L);
		assertThat(exception.retryAfterSeconds()).isNull();
		assertThat(exception.fields()).isNull();
	}

	@Test
	@DisplayName("of(409 · SOME_DOMAIN_CODE · 메시지)로 만든 예외의 overlapScheduleIds는 null이다")
	void ofWithoutOverlapHasNullOverlapScheduleIds() {
		BusinessException exception = BusinessException.of(HttpStatus.CONFLICT, "SOME_DOMAIN_CODE", "도메인 규칙에 맞지 않는 요청입니다");

		assertThat(exception.overlapScheduleIds()).isNull();
	}

	@Test
	@DisplayName("of(409 · SOME_DOMAIN_CODE · 메시지)로 만든 예외는 추가 키(details)가 비어 있고 session은 null이다")
	void ofWithoutDetailsHasEmptyDetailsAndNullSession() {
		BusinessException exception = BusinessException.of(HttpStatus.CONFLICT, "SOME_DOMAIN_CODE", "도메인 규칙에 맞지 않는 요청입니다");

		assertThat(exception.details()).isEmpty();
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("of(422 · CALL_GUARD_UNMET · 메시지).withDetail(unmet, [G2, G4])로 만든 예외는 status 422 · code · 메시지 그대로 · details에 unmet [G2, G4] 하나 · retryAfterSeconds null · fields null · session null이다")
	void withDetailAddsDetailAndKeepsStatusCodeAndMessage() {
		BusinessException exception = BusinessException
				.of(HttpStatus.UNPROCESSABLE_CONTENT, GUARD_UNMET_CODE, GUARD_UNMET_MESSAGE)
				.withDetail("unmet", List.of("G2", "G4"));

		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo(GUARD_UNMET_CODE);
		assertThat(exception.getMessage()).isEqualTo(GUARD_UNMET_MESSAGE);
		assertThat(exception.details()).containsExactly(entry("unmet", List.of("G2", "G4")));
		assertThat(exception.retryAfterSeconds()).isNull();
		assertThat(exception.fields()).isNull();
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("withDetail을 unmet · missing 순서로 두 번 이으면 details에 두 키가 넣은 순서대로 모두 남는다")
	void chainedWithDetailKeepsAllKeysInInsertionOrder() {
		BusinessException exception = BusinessException
				.of(HttpStatus.UNPROCESSABLE_CONTENT, GUARD_UNMET_CODE, GUARD_UNMET_MESSAGE)
				.withDetail("unmet", List.of("G2"))
				.withDetail("missing", List.of("PICKUP_SPOT"));

		assertThat(exception.details()).containsExactly(
				entry("unmet", List.of("G2")),
				entry("missing", List.of("PICKUP_SPOT")));
	}

	@ParameterizedTest(name = "키 {0}")
	@ValueSource(strings = {"code", "message", "retryAfterSeconds", "fields"})
	@DisplayName("withDetail에 공통 오류 키(code · message · retryAfterSeconds · fields)를 넣으면 IllegalArgumentException으로 거절한다")
	void withDetailRejectsCommonErrorKeys(String reservedKey) {
		BusinessException exception = BusinessException
				.of(HttpStatus.UNPROCESSABLE_CONTENT, GUARD_UNMET_CODE, GUARD_UNMET_MESSAGE);

		assertThatThrownBy(() -> exception.withDetail(reservedKey, "x"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("tooManyRequests(ErrorCode.RESEND_TOO_SOON, 30)로 만든 예외는 status 429 · code RESEND_TOO_SOON · 메시지 「잠시 뒤에 다시 보내 주세요」 · retryAfterSeconds 30 · fields null이다")
	void tooManyRequestsWithResendTooSoonCarriesCodeAndRetryAfterSeconds() {
		BusinessException exception = BusinessException.tooManyRequests(ErrorCode.RESEND_TOO_SOON, 30);

		assertThat(exception.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exception.code()).isEqualTo("RESEND_TOO_SOON");
		assertThat(exception.getMessage()).isEqualTo("잠시 뒤에 다시 보내 주세요");
		assertThat(exception.retryAfterSeconds()).isEqualTo(30);
		assertThat(exception.fields()).isNull();
	}

	@Test
	@DisplayName("도메인 code용 tooManyRequests(LINK_CODE_REISSUE_LIMIT · 메시지 · 300)로 만든 예외는 status 429 · code LINK_CODE_REISSUE_LIMIT · 메시지 그대로 · retryAfterSeconds 300 · fields null · overlapScheduleIds null · details 비어 있음이다")
	void tooManyRequestsWithDomainCodeCarriesCodeMessageAndRetryAfterSeconds() {
		BusinessException exception = BusinessException.tooManyRequests(
				"LINK_CODE_REISSUE_LIMIT", "연결 코드 재발급 한도를 넘었습니다", 300);

		assertThat(exception.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exception.code()).isEqualTo("LINK_CODE_REISSUE_LIMIT");
		assertThat(exception.getMessage()).isEqualTo("연결 코드 재발급 한도를 넘었습니다");
		assertThat(exception.retryAfterSeconds()).isEqualTo(300);
		assertThat(exception.fields()).isNull();
		assertThat(exception.overlapScheduleIds()).isNull();
		assertThat(exception.details()).isEmpty();
	}

	@Test
	@DisplayName("도메인 code용 tooManyRequests로 만든 예외에도 withDetail로 추가 키(nextAllowedAt)를 붙일 수 있고 retryAfterSeconds는 그대로다")
	void tooManyRequestsWithDomainCodeKeepsRetryAfterSecondsWithExtraDetail() {
		Instant nextAllowedAt = Instant.parse("2026-11-05T09:10:00Z");

		BusinessException exception = BusinessException.tooManyRequests(
				"HELP_REQUEST_TOO_SOON", "잠시 후 다시 요청해 주세요", 30)
				.withDetail("nextAllowedAt", nextAllowedAt);

		assertThat(exception.retryAfterSeconds()).isEqualTo(30);
		assertThat(exception.details()).containsOnly(entry("nextAllowedAt", nextAllowedAt));
	}

	@Test
	@DisplayName("of(429 · HELP_REQUEST_TOO_SOON · 메시지)처럼 of(HttpStatus, code, message)에 429를 넣으면 IllegalArgumentException으로 막아 retryAfterSeconds 없는 429를 만들 수 없다(429는 tooManyRequests로만, D3)")
	void ofWithTooManyRequestsStatusIsRejected() {
		assertThatThrownBy(() -> BusinessException.of(HttpStatus.TOO_MANY_REQUESTS, "HELP_REQUEST_TOO_SOON",
				"잠시 후 다시 요청해 주세요"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("of(HttpStatus, code, message)는 429가 아닌 상태(502 · MAP_API_UNAVAILABLE)는 지금처럼 막지 않고 넣은 상태 · code · message로 만든다")
	void ofWithNonTooManyRequestsStatusIsStillAllowed() {
		BusinessException exception = BusinessException.of(HttpStatus.BAD_GATEWAY, "MAP_API_UNAVAILABLE",
				"지도 검색을 잠시 사용할 수 없습니다");

		assertThat(exception.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(exception.code()).isEqualTo("MAP_API_UNAVAILABLE");
		assertThat(exception.getMessage()).isEqualTo("지도 검색을 잠시 사용할 수 없습니다");
		assertThat(exception.retryAfterSeconds()).isNull();
	}

	@Test
	@DisplayName("of(ErrorCode)에 429 코드(TOO_MANY_REQUESTS)를 넣으면 IllegalArgumentException으로 막는다(D3)")
	void ofErrorCodeWithTooManyRequestsIsRejected() {
		assertThatThrownBy(() -> BusinessException.of(ErrorCode.TOO_MANY_REQUESTS))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("of(ErrorCode)에 429 코드(RESEND_TOO_SOON)를 넣으면 IllegalArgumentException으로 막는다(D3)")
	void ofErrorCodeWithResendTooSoonIsRejected() {
		assertThatThrownBy(() -> BusinessException.of(ErrorCode.RESEND_TOO_SOON))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("of(ErrorCode.NOT_FOUND)처럼 429가 아닌 코드는 지금처럼 만들어진다")
	void ofErrorCodeWithNonTooManyRequestsIsStillAllowed() {
		BusinessException exception = BusinessException.of(ErrorCode.NOT_FOUND);

		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		assertThat(exception.retryAfterSeconds()).isNull();
	}

	@Test
	@DisplayName("tooManyRequests(ErrorCode, int)에 429가 아닌 코드(NOT_FOUND)를 넣으면 IllegalArgumentException으로 막는다(D3)")
	void tooManyRequestsWithNon429ErrorCodeIsRejected() {
		assertThatThrownBy(() -> BusinessException.tooManyRequests(ErrorCode.NOT_FOUND, 30))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("stateVersionConflict(세션 뷰)로 만든 예외는 status 409 · code STATE_VERSION_CONFLICT · 비어 있지 않은 메시지 · session에 넘긴 세션 뷰 그대로 · details 비어 있음 · retryAfterSeconds null · fields null이다")
	void stateVersionConflictCarriesSessionView() {
		Object latestSessionView = new Object();

		BusinessException exception = BusinessException.stateVersionConflict(latestSessionView);

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo(STATE_VERSION_CONFLICT);
		assertThat(exception.getMessage()).isNotBlank();
		assertThat(exception.session()).isSameAs(latestSessionView);
		assertThat(exception.details()).isEmpty();
		assertThat(exception.retryAfterSeconds()).isNull();
		assertThat(exception.fields()).isNull();
	}

}

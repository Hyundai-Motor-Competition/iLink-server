package com.example.pickupservice.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식을 확인해 주세요"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다"),
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다"),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 본문 형식입니다"),
	STATE_VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 곳에서 먼저 바뀌었습니다. 최신 상태를 확인해 주세요"),
	IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "이미 사용한 Idempotency-Key입니다"),
	VEHICLE_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 보호자가 연결된 차량입니다"),
	VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "입력값을 확인해 주세요"),
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다");

	private final HttpStatus status;
	private final String message;

}

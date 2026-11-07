package com.example.pickupservice.common.error;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final Integer retryAfterSeconds;

	private BusinessException(StatusCode statusCode, String message, Integer retryAfterSeconds) {
		super(message);
		this.status = statusCode.status();
		this.code = statusCode.code();
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public static BusinessException of(ErrorCode errorCode) {
		return new BusinessException(StatusCode.from(errorCode), errorCode.getMessage(), null);
	}

	public static BusinessException of(HttpStatus status, String code, String message) {
		return new BusinessException(new StatusCode(status, code), message, null);
	}

	public static BusinessException tooManyRequests(int retryAfterSeconds) {
		ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
		return new BusinessException(StatusCode.from(errorCode), errorCode.getMessage(), retryAfterSeconds);
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public Integer retryAfterSeconds() {
		return retryAfterSeconds;
	}

	private record StatusCode(HttpStatus status, String code) {

		private static StatusCode from(ErrorCode errorCode) {
			return new StatusCode(errorCode.getStatus(), errorCode.name());
		}
	}

}

package com.example.pickupservice.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ErrorResponse(ErrorBody error) {

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fields) {
		return new ErrorResponse(new ErrorBody(errorCode.name(), errorCode.getMessage(), null, fields));
	}

	public static ErrorResponse of(String code, String message, Integer retryAfterSeconds) {
		return new ErrorResponse(new ErrorBody(code, message, retryAfterSeconds, null));
	}

	public static ErrorResponse from(BusinessException exception) {
		return new ErrorResponse(new ErrorBody(exception.code(), exception.getMessage(),
				exception.retryAfterSeconds(), exception.fields()));
	}

	public record ErrorBody(
			String code,
			String message,
			Integer retryAfterSeconds,
			@JsonInclude(JsonInclude.Include.NON_NULL) List<FieldError> fields) {
	}

	public record FieldError(String field, String reason) {
	}

}

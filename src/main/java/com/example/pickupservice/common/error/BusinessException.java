package com.example.pickupservice.common.error;

import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import org.springframework.http.HttpStatus;

import java.util.List;

public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final Integer retryAfterSeconds;
	private final List<FieldError> fields;

	private BusinessException(StatusCode statusCode, String message, Details details) {
		super(message);
		this.status = statusCode.status();
		this.code = statusCode.code();
		this.retryAfterSeconds = details.retryAfterSeconds();
		this.fields = details.fields();
	}

	public static BusinessException of(ErrorCode errorCode) {
		return new BusinessException(StatusCode.from(errorCode), errorCode.getMessage(), Details.NONE);
	}

	public static BusinessException of(HttpStatus status, String code, String message) {
		return new BusinessException(new StatusCode(status, code), message, Details.NONE);
	}

	public static BusinessException tooManyRequests(int retryAfterSeconds) {
		ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
		return new BusinessException(StatusCode.from(errorCode), errorCode.getMessage(),
				new Details(retryAfterSeconds, null));
	}

	public static BusinessException invalidField(String field, String reason) {
		ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
		return new BusinessException(StatusCode.from(errorCode), errorCode.getMessage(),
				new Details(null, List.of(new FieldError(field, reason))));
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

	public List<FieldError> fields() {
		return fields;
	}

	private record StatusCode(HttpStatus status, String code) {

		private static StatusCode from(ErrorCode errorCode) {
			return new StatusCode(errorCode.getStatus(), errorCode.name());
		}
	}

	private record Details(Integer retryAfterSeconds, List<FieldError> fields) {

		private static final Details NONE = new Details(null, null);
	}

}

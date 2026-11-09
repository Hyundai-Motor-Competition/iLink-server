package com.example.pickupservice.common.error;

import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BusinessException extends RuntimeException {

	private static final Set<String> RESERVED_DETAIL_KEYS = Set.of("code", "message", "retryAfterSeconds", "fields");

	private final HttpStatus status;
	private final String code;
	private final Integer retryAfterSeconds;
	private final List<FieldError> fields;
	private final List<Long> overlapScheduleIds;
	private final Map<String, Object> details = new LinkedHashMap<>();
	private Object session;

	private BusinessException(StatusCode statusCode, String message, Details details) {
		super(message);
		this.status = statusCode.status();
		this.code = statusCode.code();
		this.retryAfterSeconds = details.retryAfterSeconds();
		this.fields = details.fields();
		this.overlapScheduleIds = details.overlapScheduleIds();
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
				new Details(retryAfterSeconds, null, null));
	}

	public static BusinessException tooManyRequests(ErrorCode errorCode, int retryAfterSeconds) {
		return new BusinessException(StatusCode.from(errorCode), errorCode.getMessage(),
				new Details(retryAfterSeconds, null, null));
	}

	public static BusinessException invalidField(String field, String reason) {
		ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
		return new BusinessException(StatusCode.from(errorCode), errorCode.getMessage(),
				new Details(null, List.of(new FieldError(field, reason)), null));
	}

	public static BusinessException conflictWithOverlap(HttpStatus status, String code, String message,
			List<Long> overlapScheduleIds) {
		return new BusinessException(new StatusCode(status, code), message,
				new Details(null, null, List.copyOf(overlapScheduleIds)));
	}

	public static BusinessException stateVersionConflict(Object session) {
		BusinessException exception = of(ErrorCode.STATE_VERSION_CONFLICT);
		exception.session = session;
		return exception;
	}

	public BusinessException withDetail(String key, Object value) {
		if (RESERVED_DETAIL_KEYS.contains(key)) {
			throw new IllegalArgumentException("공통 오류 키는 추가 키로 쓸 수 없습니다: " + key);
		}
		details.put(key, value);
		return this;
	}

	public Map<String, Object> details() {
		return Collections.unmodifiableMap(details);
	}

	public Object session() {
		return session;
	}

	public List<Long> overlapScheduleIds() {
		return overlapScheduleIds;
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

	private record Details(Integer retryAfterSeconds, List<FieldError> fields, List<Long> overlapScheduleIds) {

		private static final Details NONE = new Details(null, null, null);
	}

}

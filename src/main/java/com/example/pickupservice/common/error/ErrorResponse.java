package com.example.pickupservice.common.error;

import com.example.pickupservice.common.time.TimeUtils;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ErrorResponse(ErrorBody error, @JsonInclude(JsonInclude.Include.NON_NULL) Object session) {

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fields) {
		return new ErrorResponse(new ErrorBody(errorCode.name(), errorCode.getMessage(), null, fields, null, Map.of()), null);
	}

	public static ErrorResponse of(String code, String message, Integer retryAfterSeconds) {
		return new ErrorResponse(new ErrorBody(code, message, retryAfterSeconds, null, null, Map.of()), null);
	}

	public static ErrorResponse from(BusinessException exception) {
		return new ErrorResponse(new ErrorBody(exception.code(), exception.getMessage(),
				exception.retryAfterSeconds(), exception.fields(), exception.overlapScheduleIds(),
				toResponseValues(exception.details())), exception.session());
	}

	private static Map<String, Object> toResponseValues(Map<String, Object> details) {
		Map<String, Object> values = new LinkedHashMap<>();
		details.forEach((key, value) -> values.put(key, value instanceof Instant instant ? TimeUtils.toKst(instant) : value));
		return values;
	}

	public record ErrorBody(
			String code,
			String message,
			Integer retryAfterSeconds,
			@JsonInclude(JsonInclude.Include.NON_NULL) List<FieldError> fields,
			@JsonInclude(JsonInclude.Include.NON_NULL) List<Long> overlapScheduleIds,
			@JsonIgnore Map<String, Object> details) {

		@JsonAnyGetter
		public Map<String, Object> detailsForJson() {
			return details;
		}
	}

	public record FieldError(String field, String reason) {
	}

}

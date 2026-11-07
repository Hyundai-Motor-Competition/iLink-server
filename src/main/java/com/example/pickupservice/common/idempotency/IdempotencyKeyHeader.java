package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.error.ErrorResponse.FieldError;

import java.util.Optional;

final class IdempotencyKeyHeader {

	static final String NAME = "Idempotency-Key";
	static final int MAX_LENGTH = 255;

	private IdempotencyKeyHeader() {
	}

	static Optional<FieldError> violationOf(String value) {
		if (value == null || value.isBlank()) {
			return Optional.of(new FieldError(NAME, "값이 필요합니다"));
		}
		if (value.length() > MAX_LENGTH) {
			return Optional.of(new FieldError(NAME, MAX_LENGTH + "자 이하여야 합니다"));
		}
		return Optional.empty();
	}

}

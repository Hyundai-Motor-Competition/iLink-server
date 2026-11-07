package com.example.pickupservice.common.idempotency;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class IdempotencyKeyStore {

	private final IdempotencyKeyRepository idempotencyKeyRepository;

	Optional<IdempotencyKeyEntity> reserve(IdempotentRequest request) {
		try {
			return Optional.of(idempotencyKeyRepository.save(new IdempotencyKeyEntity(request)));
		} catch (DataIntegrityViolationException violation) {
			if (!isUniqueViolation(violation)) {
				throw violation;
			}
			return Optional.empty();
		}
	}

	private static boolean isUniqueViolation(DataIntegrityViolationException violation) {
		for (Throwable cause = violation; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraintViolation) {
				return constraintViolation.getKind() == ConstraintKind.UNIQUE;
			}
		}
		return false;
	}

	Optional<IdempotencyKeyEntity> findReplayable(IdempotentRequest request) {
		return idempotencyKeyRepository.findByPrincipalTypeAndPrincipalIdAndIdempotencyKey(
						request.principalType(), request.principalId(), request.key())
				.filter(stored -> stored.isReplayableFor(request.requestHash()));
	}

	void completeOrRelease(IdempotencyKeyEntity reserved, ContentCachingResponseWrapper response) {
		if (!HttpStatusCode.valueOf(response.getStatus()).is2xxSuccessful()) {
			release(reserved);
			return;
		}
		reserved.complete(response.getStatus(), bodyOf(response));
		idempotencyKeyRepository.save(reserved);
	}

	void release(IdempotencyKeyEntity reserved) {
		idempotencyKeyRepository.delete(reserved);
	}

	private static String bodyOf(ContentCachingResponseWrapper response) {
		byte[] body = response.getContentAsByteArray();
		return body.length == 0 ? null : new String(body, StandardCharsets.UTF_8);
	}

}

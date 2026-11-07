package com.example.pickupservice.common.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Component
@RequiredArgsConstructor
class IdempotencyKeyCleaner {

	private static final Duration RETENTION = Duration.ofHours(24);

	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${pickup.idempotency.cleanup-interval}",
			initialDelayString = "${pickup.idempotency.cleanup-interval}")
	@Transactional
	int deleteExpired() {
		return idempotencyKeyRepository.deleteByCreatedAtBefore(clock.instant().minus(RETENTION));
	}

}

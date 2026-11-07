package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
class LinkStatusPollGuard {

	private static final Duration MIN_INTERVAL = Duration.ofSeconds(1);
	private static final int RETRY_AFTER_SECONDS = 1;
	private static final int CLEANUP_THRESHOLD = 10_000;

	private final Clock clock;
	private final Map<Long, Instant> lastPolledAt = new ConcurrentHashMap<>();

	void check(long vehicleId) {
		Instant now = clock.instant();
		removeStaleEntries(now);
		AtomicBoolean tooSoon = new AtomicBoolean(false);
		lastPolledAt.compute(vehicleId, (id, last) -> {
			if (last != null && now.isBefore(last.plus(MIN_INTERVAL))) {
				tooSoon.set(true);
				return last;
			}
			return now;
		});
		if (tooSoon.get()) {
			throw BusinessException.tooManyRequests(RETRY_AFTER_SECONDS);
		}
	}

	private void removeStaleEntries(Instant now) {
		if (lastPolledAt.size() < CLEANUP_THRESHOLD) {
			return;
		}
		Instant threshold = now.minus(MIN_INTERVAL);
		lastPolledAt.values().removeIf(last -> last.isBefore(threshold));
	}

}

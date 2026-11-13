package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.common.time.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LinkCodeAttemptLimiter {

	private static final int MAX_FAILURES = 5;
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(1);

	private final Clock clock;
	private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();
	private final Map<String, Instant> blockedAts = new ConcurrentHashMap<>();

	public int secondsUntilUnblocked(String clientKey) {
		Instant blockedAt = blockedAts.get(clientKey);
		if (blockedAt == null) {
			return 0;
		}
		Instant now = Instant.now(clock);
		Instant unblockedAt = blockedAt.plus(BLOCK_DURATION);
		if (now.isBefore(unblockedAt)) {
			return TimeUtils.secondsUntil(now, unblockedAt);
		}
		blockedAts.remove(clientKey);
		failureCounts.remove(clientKey);
		return 0;
	}

	public void recordFailure(String clientKey) {
		int failures = failureCounts.merge(clientKey, 1, Integer::sum);
		if (failures >= MAX_FAILURES) {
			blockedAts.put(clientKey, Instant.now(clock));
		}
	}
}

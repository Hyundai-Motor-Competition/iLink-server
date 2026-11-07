package com.example.pickupservice.auth.application.service;

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

	public boolean isBlocked(String clientKey) {
		Instant blockedAt = blockedAts.get(clientKey);
		if (blockedAt == null) {
			return false;
		}
		if (Instant.now(clock).isBefore(blockedAt.plus(BLOCK_DURATION))) {
			return true;
		}
		blockedAts.remove(clientKey);
		failureCounts.remove(clientKey);
		return false;
	}

	public void recordFailure(String clientKey) {
		int failures = failureCounts.merge(clientKey, 1, Integer::sum);
		if (failures >= MAX_FAILURES) {
			blockedAts.put(clientKey, Instant.now(clock));
		}
	}
}

package com.example.pickupservice.auth.application.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LinkCodeIssueCounter {

	private static final int LIMIT = 5;

	private final Map<Long, Integer> issueCounts = new ConcurrentHashMap<>();

	public boolean isLimitReached(long deviceId) {
		return issueCounts.getOrDefault(deviceId, 0) >= LIMIT;
	}

	public void increment(long deviceId) {
		issueCounts.merge(deviceId, 1, Integer::sum);
	}

	public int remaining(long deviceId) {
		return LIMIT - issueCounts.getOrDefault(deviceId, 0);
	}

	public void reset(long deviceId) {
		issueCounts.remove(deviceId);
	}
}

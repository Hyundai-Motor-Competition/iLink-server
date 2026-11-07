package com.example.pickupservice.auth.application.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LinkCodeIssueCounter {

	private static final int LIMIT = 5;

	private final Map<Long, Integer> issueCounts = new ConcurrentHashMap<>();

	public boolean isLimitReached(long vehicleId) {
		return issueCounts.getOrDefault(vehicleId, 0) >= LIMIT;
	}

	public void increment(long vehicleId) {
		issueCounts.merge(vehicleId, 1, Integer::sum);
	}

	public int remaining(long vehicleId) {
		return LIMIT - issueCounts.getOrDefault(vehicleId, 0);
	}

	public void reset(long vehicleId) {
		issueCounts.remove(vehicleId);
	}
}

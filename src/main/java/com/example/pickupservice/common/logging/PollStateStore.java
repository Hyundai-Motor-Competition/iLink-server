package com.example.pickupservice.common.logging;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

class PollStateStore {

	private static final int MAX_KEYS = 10_000;
	private static final long TTL_NANOS = Duration.ofMinutes(10).toNanos();

	private final Map<String, StoredState> states = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, StoredState> eldest) {
			return size() > MAX_KEYS;
		}
	};

	synchronized String put(String key, String state) {
		long now = System.nanoTime();
		StoredState previous = states.put(key, new StoredState(state, now));
		return previous == null || previous.isExpiredAt(now) ? null : previous.state();
	}

	private record StoredState(String state, long storedAtNanos) {

		boolean isExpiredAt(long nowNanos) {
			return nowNanos - storedAtNanos > TTL_NANOS;
		}

	}

}

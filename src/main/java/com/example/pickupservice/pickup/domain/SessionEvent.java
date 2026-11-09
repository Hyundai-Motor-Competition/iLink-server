package com.example.pickupservice.pickup.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionEvent {

	private long eventId;
	private long sessionId;
	private long seq;
	private SessionEventType type;
	private int stateVersion;
	private Instant occurredAt;
	private Map<String, Object> payload;

	public static SessionEvent restore(long eventId, long sessionId, long seq, SessionEventType type,
			int stateVersion, Instant occurredAt, Map<String, Object> payload) {
		return new SessionEvent(eventId, sessionId, seq, type, stateVersion, occurredAt, payload);
	}
}

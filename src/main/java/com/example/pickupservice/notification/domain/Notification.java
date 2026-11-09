package com.example.pickupservice.notification.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification {

	private Long notificationId;
	private long sessionId;
	private Long callId;
	private String type;
	private long deviceId;
	private Instant sentAt;
	private Instant deliveredAt;
	private Instant openedAt;
	private Instant createdAt;

	public static Notification create(long sessionId, Long callId, String type, long deviceId) {
		return new Notification(null, sessionId, callId, type, deviceId, null, null, null, null);
	}

	public static Notification restore(Long notificationId, long sessionId, Long callId, String type, long deviceId,
			Instant sentAt, Instant deliveredAt, Instant openedAt) {
		return new Notification(notificationId, sessionId, callId, type, deviceId, sentAt, deliveredAt, openedAt, null);
	}

	public static Notification restore(Long notificationId, long sessionId, Long callId, String type, long deviceId,
			Instant sentAt, Instant deliveredAt, Instant openedAt, Instant createdAt) {
		return new Notification(notificationId, sessionId, callId, type, deviceId, sentAt, deliveredAt, openedAt,
				createdAt);
	}

	public void markSent(Instant at) {
		this.sentAt = at;
	}

	public void markDelivered(Instant receivedAt) {
		if (this.deliveredAt == null) {
			this.deliveredAt = receivedAt;
		}
	}
}

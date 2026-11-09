package com.example.pickupservice.notification.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;

public final class ResendPolicy {

	private ResendPolicy() {
	}

	private static final long RESEND_INTERVAL_SECONDS = 60L;
	private static final int MAX_ATTEMPT = 3;

	public static boolean shouldResend(Notification notification, int attempt, boolean callAcknowledged) {
		if (attempt >= MAX_ATTEMPT) {
			return false;
		}
		if ("CALL".equals(notification.getType())) {
			return !callAcknowledged;
		}
		return notification.getDeliveredAt() == null;
	}

	public static Instant lastAttemptAt(Collection<Notification> bundle) {
		Notification latest = bundle.stream()
				.max(Comparator.comparingLong(Notification::getNotificationId))
				.orElseThrow();
		return latest.getSentAt() != null ? latest.getSentAt() : latest.getCreatedAt();
	}

	public static void ensureResendAllowed(Instant lastSentAt, Instant now) {
		long elapsed = now.getEpochSecond() - lastSentAt.getEpochSecond();
		if (elapsed < RESEND_INTERVAL_SECONDS) {
			throw new NotificationRuleException(NotificationViolation.RESEND_TOO_SOON,
					(int) (RESEND_INTERVAL_SECONDS - elapsed));
		}
	}
}

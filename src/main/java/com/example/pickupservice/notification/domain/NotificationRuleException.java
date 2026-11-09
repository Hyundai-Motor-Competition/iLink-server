package com.example.pickupservice.notification.domain;

public class NotificationRuleException extends RuntimeException {

	private final NotificationViolation violation;
	private final int retryAfterSeconds;

	public NotificationRuleException(NotificationViolation violation, int retryAfterSeconds) {
		super(violation.name());
		this.violation = violation;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public NotificationViolation violation() {
		return violation;
	}

	public int retryAfterSeconds() {
		return retryAfterSeconds;
	}
}

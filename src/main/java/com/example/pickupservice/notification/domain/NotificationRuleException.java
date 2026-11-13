package com.example.pickupservice.notification.domain;

public class NotificationRuleException extends RuntimeException {

	private final NotificationViolation violation;

	public NotificationRuleException(NotificationViolation violation) {
		super(violation.name());
		this.violation = violation;
	}

	public NotificationViolation violation() {
		return violation;
	}
}

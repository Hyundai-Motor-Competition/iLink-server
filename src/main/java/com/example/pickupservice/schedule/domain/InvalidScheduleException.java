package com.example.pickupservice.schedule.domain;

public class InvalidScheduleException extends RuntimeException {

	private final String field;
	private final String reason;

	public InvalidScheduleException(String field, String reason) {
		super(field + ": " + reason);
		this.field = field;
		this.reason = reason;
	}

	public String field() {
		return field;
	}

	public String reason() {
		return reason;
	}
}

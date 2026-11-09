package com.example.pickupservice.pickup.domain;

public class PickupRuleException extends RuntimeException {

	private final PickupViolation violation;

	public PickupRuleException(PickupViolation violation) {
		super(violation.name());
		this.violation = violation;
	}

	public PickupViolation violation() {
		return violation;
	}
}

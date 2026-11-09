package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class ValidPrepMinutesValidator implements ConstraintValidator<ValidPrepMinutes, Integer> {

	private static final Set<Integer> ALLOWED_MINUTES = Set.of(5, 10, 15);

	@Override
	public boolean isValid(Integer minutes, ConstraintValidatorContext context) {
		return minutes == null || ALLOWED_MINUTES.contains(minutes);
	}

}

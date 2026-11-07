package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.user.domain.PhoneRule;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneValidator implements ConstraintValidator<ValidPhone, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || PhoneRule.isValid(value);
	}

}

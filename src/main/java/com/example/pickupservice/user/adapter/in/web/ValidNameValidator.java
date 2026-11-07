package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.text.NameRule;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidNameValidator implements ConstraintValidator<ValidName, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || NameRule.isValid(value);
	}

}

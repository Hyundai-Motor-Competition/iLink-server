package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.domain.VinRule;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidVinValidator implements ConstraintValidator<ValidVin, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return VinRule.isValid(value);
	}

}

package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.domain.Platform;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GuardianPlatformValidator implements ConstraintValidator<GuardianPlatform, Platform> {

	@Override
	public boolean isValid(Platform value, ConstraintValidatorContext context) {
		return value == Platform.ANDROID || value == Platform.IOS;
	}

}

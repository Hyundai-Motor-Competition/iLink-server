package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.user.domain.NameRule;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidGuardianNameValidator implements ConstraintValidator<ValidGuardianName, ApproveLinkRequest> {

	private static final String NAME_FIELD = "name";

	@Override
	public boolean isValid(ApproveLinkRequest request, ConstraintValidatorContext context) {
		if (request == null || request.mode() != LinkMode.NEW || NameRule.isValid(request.name())) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addPropertyNode(NAME_FIELD)
				.addConstraintViolation();
		return false;
	}

}

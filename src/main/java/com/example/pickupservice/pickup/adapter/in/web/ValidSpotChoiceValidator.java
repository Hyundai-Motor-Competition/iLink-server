package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidSpotChoiceValidator implements ConstraintValidator<ValidSpotChoice, ConfirmRouteRequest> {

	private static final String SPOT_ID_FIELD = "spotId";

	@Override
	public boolean isValid(ConfirmRouteRequest request, ConstraintValidatorContext context) {
		if (request == null || (request.spotId() == null) != (request.candidate() == null)) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addPropertyNode(SPOT_ID_FIELD)
				.addConstraintViolation();
		return false;
	}

}

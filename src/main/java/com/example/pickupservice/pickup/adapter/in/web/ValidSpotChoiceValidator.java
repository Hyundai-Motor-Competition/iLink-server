package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidSpotChoiceValidator implements ConstraintValidator<ValidSpotChoice, SpotChoiceRequest> {

	private static final String SPOT_ID_FIELD = "spotId";
	private static final String HERE_FIELD = "here";
	private static final String HERE_ALONE_MESSAGE = "here는 spotId · candidate와 함께 보낼 수 없습니다";

	@Override
	public boolean isValid(SpotChoiceRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		if (Boolean.TRUE.equals(request.here())) {
			if (request.spotId() == null && request.candidate() == null) {
				return true;
			}
			return reject(context, HERE_ALONE_MESSAGE, HERE_FIELD);
		}
		if ((request.spotId() == null) != (request.candidate() == null)) {
			return true;
		}
		return reject(context, context.getDefaultConstraintMessageTemplate(), SPOT_ID_FIELD);
	}

	private static boolean reject(ConstraintValidatorContext context, String message, String field) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode(field)
				.addConstraintViolation();
		return false;
	}

}

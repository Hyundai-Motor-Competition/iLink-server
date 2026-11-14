package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSpotChoiceValidator.class)
public @interface ValidSpotChoice {

	String message() default "spotId와 candidate 중 하나만 보내야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

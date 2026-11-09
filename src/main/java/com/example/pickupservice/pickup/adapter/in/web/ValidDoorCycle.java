package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDoorCycleValidator.class)
public @interface ValidDoorCycle {

	String message() default "DOOR_CYCLE이면 FRONT_RIGHT · REAR_LEFT · REAR_RIGHT 중 하나여야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

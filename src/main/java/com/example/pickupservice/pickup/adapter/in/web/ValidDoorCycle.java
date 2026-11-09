package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * type이 DOOR_CYCLE이면 door는 FRONT_RIGHT · REAR_LEFT · REAR_RIGHT 중 하나여야 한다(운전석 제외).
 * 오류는 door 필드에 붙는다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDoorCycleValidator.class)
public @interface ValidDoorCycle {

	String message() default "DOOR_CYCLE이면 FRONT_RIGHT · REAR_LEFT · REAR_RIGHT 중 하나여야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 준비 시간 연장 분은 5 · 10 · 15 중 하나여야 한다. null은 @NotNull이 거른다.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPrepMinutesValidator.class)
public @interface ValidPrepMinutes {

	String message() default "5, 10, 15분 중 하나여야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

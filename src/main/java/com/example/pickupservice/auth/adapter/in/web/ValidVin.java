package com.example.pickupservice.auth.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidVinValidator.class)
public @interface ValidVin {

	String message() default "17자 영대문자 · 숫자여야 하고 I · O · Q는 쓸 수 없습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

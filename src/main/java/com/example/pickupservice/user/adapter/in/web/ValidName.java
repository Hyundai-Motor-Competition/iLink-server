package com.example.pickupservice.user.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidNameValidator.class)
public @interface ValidName {

	String message() default "1~20자여야 하고 이모지 · 제어문자는 쓸 수 없습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

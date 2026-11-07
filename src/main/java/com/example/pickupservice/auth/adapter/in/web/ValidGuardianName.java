package com.example.pickupservice.auth.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidGuardianNameValidator.class)
public @interface ValidGuardianName {

	String message() default "mode NEW에는 1~20자 이름이 필요하고 이모지 · 제어 문자는 쓸 수 없습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

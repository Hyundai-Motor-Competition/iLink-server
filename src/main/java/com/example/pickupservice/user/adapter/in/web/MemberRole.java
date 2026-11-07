package com.example.pickupservice.user.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MemberRoleValidator.class)
public @interface MemberRole {

	String message() default "CHILD · CO_GUARDIAN · DELEGATE · TEACHER 중 하나여야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}

package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.security.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MemberRoleValidator implements ConstraintValidator<MemberRole, Role> {

	@Override
	public boolean isValid(Role value, ConstraintValidatorContext context) {
		return value != Role.GUARDIAN;
	}

}

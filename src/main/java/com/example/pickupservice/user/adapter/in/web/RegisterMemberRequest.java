package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.RegisterMemberCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterMemberRequest(
		@NotNull(message = "필수 값입니다") @MemberRole Role role,
		@NotNull(message = "필수 값입니다") @ValidName String name,
		@ValidPhone String phone,
		@Size(min = 1, max = 20, message = "1~20자여야 합니다") String grade
) {

	public RegisterMemberCommand toCommand(AuthPrincipal requester) {
		return new RegisterMemberCommand(requester.familyId(), requester.role(), role, name, phone).withGrade(grade);
	}

}

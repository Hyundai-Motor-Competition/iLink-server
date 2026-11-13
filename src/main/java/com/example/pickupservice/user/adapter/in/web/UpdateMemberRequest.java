package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.UpdateMemberCommand;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
		@ValidName String name,
		@ValidPhone String phone,
		@MemberRole Role role,
		@Size(min = 1, max = 20, message = "1~20자여야 합니다") String grade
) {

	public UpdateMemberCommand toCommand(AuthPrincipal requester, long userId) {
		return new UpdateMemberCommand(
				requester.familyId(), requester.id(), requester.role(), userId, name, phone, role, grade);
	}

}

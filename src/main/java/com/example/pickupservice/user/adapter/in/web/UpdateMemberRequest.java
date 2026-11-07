package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.UpdateMemberCommand;

public record UpdateMemberRequest(
		@ValidName String name,
		@ValidPhone String phone,
		Role role
) {

	public UpdateMemberCommand toCommand(AuthPrincipal requester, long userId) {
		return new UpdateMemberCommand(
				requester.familyId(), requester.id(), requester.role(), userId, name, phone, role);
	}

}

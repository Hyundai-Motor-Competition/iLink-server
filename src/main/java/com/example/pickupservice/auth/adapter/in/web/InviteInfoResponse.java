package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.InviteInfo;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.common.time.TimeUtils;

import java.util.List;

public record InviteInfoResponse(
		String inviterName,
		String memberName,
		Role role,
		List<ConsentType> requiredConsents,
		String expiresAt
) {

	public static InviteInfoResponse from(InviteInfo info) {
		return new InviteInfoResponse(
				info.inviterName(),
				info.memberName(),
				info.role(),
				info.requiredConsents(),
				TimeUtils.toKst(info.expiresAt()));
	}

}

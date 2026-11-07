package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.AcceptInviteResult;
import com.example.pickupservice.common.security.Role;

public record AcceptInviteResponse(
		long userId,
		long familyId,
		Role role,
		String accessToken,
		String refreshToken
) {

	public static AcceptInviteResponse from(AcceptInviteResult result) {
		return new AcceptInviteResponse(
				result.userId(),
				result.familyId(),
				result.role(),
				result.accessToken(),
				result.refreshToken());
	}

}

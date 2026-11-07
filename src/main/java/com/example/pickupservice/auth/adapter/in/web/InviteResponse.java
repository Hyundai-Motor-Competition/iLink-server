package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.InviteResult;
import com.example.pickupservice.common.time.TimeUtils;

public record InviteResponse(String inviteUrl, String expiresAt) {

	public static InviteResponse from(InviteResult result) {
		return new InviteResponse(result.inviteUrl(), TimeUtils.toKst(result.expiresAt()));
	}

}

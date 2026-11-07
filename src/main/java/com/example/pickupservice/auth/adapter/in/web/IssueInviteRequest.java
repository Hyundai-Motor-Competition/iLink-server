package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.IssueInviteCommand;

public record IssueInviteRequest(Boolean replaceDevice) {

	public static IssueInviteCommand toCommand(IssueInviteRequest request, long familyId, long userId) {
		boolean replaceDevice = request != null && Boolean.TRUE.equals(request.replaceDevice());
		return new IssueInviteCommand(familyId, userId, replaceDevice);
	}

}

package com.example.pickupservice.auth.application.port.in;

public interface IssueInviteUseCase {

	InviteResult issueInvite(IssueInviteCommand command);
}

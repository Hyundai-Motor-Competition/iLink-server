package com.example.pickupservice.auth.application.port.in;

public interface AcceptInviteUseCase {

	AcceptInviteResult acceptInvite(String token, AcceptInviteCommand command);
}

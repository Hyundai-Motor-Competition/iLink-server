package com.example.pickupservice.auth.application.port.in;

public interface GetInviteUseCase {

	InviteInfo getInvite(String token);
}

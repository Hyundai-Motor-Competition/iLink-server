package com.example.pickupservice.user.application.port.in;

public record RemoveMemberCommand(
		long familyId,
		long requesterId,
		long userId
) {
}

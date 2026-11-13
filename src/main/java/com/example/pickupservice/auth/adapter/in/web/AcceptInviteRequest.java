package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.AcceptInviteCommand;
import com.example.pickupservice.auth.domain.Platform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AcceptInviteRequest(
		@NotNull List<@Valid @NotNull ConsentRequest> consents,
		@GuardianPlatform Platform platform,
		@Size(min = 1, max = 255) String pushToken
) {

	public AcceptInviteCommand toCommand() {
		return new AcceptInviteCommand(
				consents.stream().map(ConsentRequest::toAgreement).toList(),
				platform,
				pushToken);
	}

}

package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.ApproveLinkCommand;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.Platform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@ValidGuardianName
public record ApproveLinkRequest(
		@NotBlank String code,
		@NotNull LinkMode mode,
		@NotNull List<@Valid @NotNull ConsentRequest> consents,
		@GuardianPlatform Platform platform,
		@Size(max = 255) String pushToken,
		String name
) {

	public ApproveLinkCommand toCommand() {
		return new ApproveLinkCommand(
				code,
				mode,
				consents.stream().map(ConsentRequest::toAgreement).toList(),
				platform,
				pushToken,
				name);
	}

}

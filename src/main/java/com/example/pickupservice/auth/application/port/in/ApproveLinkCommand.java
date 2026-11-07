package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.Platform;

import java.util.List;

public record ApproveLinkCommand(
		String code,
		LinkMode mode,
		List<ConsentAgreement> consents,
		Platform platform,
		String pushToken,
		String name) {
}

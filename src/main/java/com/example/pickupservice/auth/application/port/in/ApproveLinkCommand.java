package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.image.ProfileImage;

import java.util.List;

public record ApproveLinkCommand(
		String code,
		LinkMode mode,
		List<ConsentAgreement> consents,
		Platform platform,
		String pushToken,
		String name,
		ProfileImage profileImage) {

	public ApproveLinkCommand(String code, LinkMode mode, List<ConsentAgreement> consents, Platform platform,
			String pushToken, String name) {
		this(code, mode, consents, platform, pushToken, name, null);
	}

	public ApproveLinkCommand withProfileImage(ProfileImage profileImage) {
		return new ApproveLinkCommand(code, mode, consents, platform, pushToken, name, profileImage);
	}
}

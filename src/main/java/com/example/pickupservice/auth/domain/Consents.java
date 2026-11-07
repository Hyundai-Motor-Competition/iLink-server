package com.example.pickupservice.auth.domain;

import java.util.Set;

import com.example.pickupservice.common.security.Role;

public final class Consents {

	private static final Set<ConsentType> CHILD_INVITE = Set.of(ConsentType.TERMS, ConsentType.PRIVACY,
			ConsentType.LOCATION, ConsentType.PUSH);
	private static final Set<ConsentType> BASIC = Set.of(ConsentType.TERMS, ConsentType.PRIVACY);

	private Consents() {
	}

	public static Set<ConsentType> requiredForInvite(Role role) {
		return role == Role.CHILD ? CHILD_INVITE : BASIC;
	}

	public static Set<ConsentType> requiredForGuardianLink() {
		return BASIC;
	}
}

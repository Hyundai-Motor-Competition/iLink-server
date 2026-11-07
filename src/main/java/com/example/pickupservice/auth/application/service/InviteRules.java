package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.Consents;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;

import java.time.Instant;
import java.util.List;

final class InviteRules {

	private InviteRules() {
	}

	static MemberInvite loadUsableInvite(LoadInvitePort loadInvitePort, String token, Instant now) {
		MemberInvite invite = loadInvitePort.loadInviteByToken(token)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		if (invite.isUsed()) {
			throw AuthErrors.inviteAlreadyUsed();
		}
		if (invite.isExpired(now)) {
			throw AuthErrors.inviteExpired();
		}
		return invite;
	}

	static List<ConsentType> requiredConsents(Role role) {
		return Consents.requiredForInvite(role).stream().sorted().toList();
	}
}

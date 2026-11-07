package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.common.security.Role;

import java.time.Instant;
import java.util.List;

public record InviteInfo(
		String inviterName,
		String memberName,
		Role role,
		List<ConsentType> requiredConsents,
		Instant expiresAt) {
}

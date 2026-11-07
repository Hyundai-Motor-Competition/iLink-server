package com.example.pickupservice.auth.domain;

import java.time.Duration;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberInvite {

	private static final Duration VALID_DURATION = Duration.ofHours(24);

	private final Long inviteId;
	private final long userId;
	private final String token;
	private Instant expiresAt;
	private Instant usedAt;

	public static MemberInvite issue(long userId, String token, Instant issuedAt) {
		return new MemberInvite(null, userId, token, issuedAt.plus(VALID_DURATION), null);
	}

	public static MemberInvite restore(Long inviteId, long userId, String token, Instant expiresAt, Instant usedAt) {
		return new MemberInvite(inviteId, userId, token, expiresAt, usedAt);
	}

	public boolean isUsed() {
		return usedAt != null;
	}

	public boolean isExpired(Instant now) {
		return !now.isBefore(expiresAt);
	}

	public void markUsed(Instant at) {
		this.usedAt = at;
	}

	public void revoke(Instant now) {
		this.expiresAt = now;
	}
}

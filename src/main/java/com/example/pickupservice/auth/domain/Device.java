package com.example.pickupservice.auth.domain;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Device {

	private final Long deviceId;
	private final OwnerType ownerType;
	private final long ownerId;
	private final Platform platform;
	private String pushToken;
	private Instant pushUpdatedAt;
	private String refreshTokenHash;
	private Instant refreshTokenExpiresAt;
	private Instant revokedAt;

	public static Device register(OwnerType ownerType, long ownerId, Platform platform, String pushToken, Instant at,
			String refreshTokenHash, Instant refreshTokenExpiresAt) {
		Instant pushUpdatedAt = pushToken == null ? null : at;
		return new Device(null, ownerType, ownerId, platform, pushToken, pushUpdatedAt, refreshTokenHash,
				refreshTokenExpiresAt, null);
	}

	public static Device restore(Long deviceId, OwnerType ownerType, long ownerId, Platform platform, String pushToken,
			Instant pushUpdatedAt, String refreshTokenHash, Instant revokedAt, Instant refreshTokenExpiresAt) {
		return new Device(deviceId, ownerType, ownerId, platform, pushToken, pushUpdatedAt, refreshTokenHash,
				refreshTokenExpiresAt, revokedAt);
	}

	public boolean isRefreshTokenExpired(Instant now) {
		return refreshTokenExpiresAt == null || !now.isBefore(refreshTokenExpiresAt);
	}

	public void rotateRefreshToken(String refreshTokenHash, Instant refreshTokenExpiresAt) {
		this.refreshTokenHash = refreshTokenHash;
		this.refreshTokenExpiresAt = refreshTokenExpiresAt;
	}

	public boolean isLive() {
		return revokedAt == null;
	}

	public void revoke(Instant at) {
		this.revokedAt = at;
	}

	public void updatePushToken(String pushToken, Instant at) {
		this.pushToken = pushToken;
		this.pushUpdatedAt = at;
	}
}

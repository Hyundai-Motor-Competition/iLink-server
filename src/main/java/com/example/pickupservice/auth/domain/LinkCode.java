package com.example.pickupservice.auth.domain;

import java.time.Duration;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LinkCode {

	private static final Duration VALID_DURATION = Duration.ofMinutes(5);

	private final Long codeId;
	private final long vehicleId;
	private final Long issuedDeviceId;
	private final boolean issuedBeforeLink;
	private final String code;
	private LinkCodeStatus status;
	private Instant expiresAt;
	private Instant usedAt;
	private LinkMode mode;
	private Instant tokenDeliveredAt;

	public static LinkCode issue(long vehicleId, long deviceId, boolean issuedBeforeLink, String code,
			Instant issuedAt) {
		return new LinkCode(null, vehicleId, deviceId, issuedBeforeLink, code, LinkCodeStatus.ISSUED,
				issuedAt.plus(VALID_DURATION), null, null, null);
	}

	public static LinkCode restore(Long codeId, long vehicleId, Long issuedDeviceId, boolean issuedBeforeLink,
			String code, LinkCodeStatus status, Instant expiresAt, Instant usedAt, LinkMode mode,
			Instant tokenDeliveredAt) {
		return new LinkCode(codeId, vehicleId, issuedDeviceId, issuedBeforeLink, code, status, expiresAt, usedAt, mode,
				tokenDeliveredAt);
	}

	public boolean isExpired(Instant now) {
		return !now.isBefore(expiresAt);
	}

	public void approve(LinkMode mode, Instant at) {
		this.mode = mode;
		this.status = LinkCodeStatus.USED;
		this.usedAt = at;
	}

	public void markTokenDelivered(Instant at) {
		this.tokenDeliveredAt = at;
	}

	public void expire() {
		this.status = LinkCodeStatus.EXPIRED;
	}
}

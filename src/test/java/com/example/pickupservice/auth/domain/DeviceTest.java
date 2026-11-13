package com.example.pickupservice.auth.domain;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTest {

	private static final Instant REGISTERED_AT = Instant.parse("2026-11-05T12:05:00Z");
	private static final String HASH = "a".repeat(64);
	private static final String NEW_HASH = "b".repeat(64);
	private static final Instant EXPIRES_AT = REGISTERED_AT.plus(Duration.ofDays(14));

	@Test
	@DisplayName("Device.register는 pushToken이 있으면 pushUpdatedAt을 등록 시각으로 채우고 deviceId · revokedAt은 비워 둔다")
	void registerWithPushTokenFillsPushUpdatedAtAndLeavesDeviceIdAndRevokedAtEmpty() {
		Device device = Device.register(OwnerType.USER, 31L, Platform.ANDROID, "fcm-token-1", REGISTERED_AT, HASH,
				EXPIRES_AT);

		assertThat(device.getDeviceId()).isNull();
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.USER);
		assertThat(device.getOwnerId()).isEqualTo(31L);
		assertThat(device.getPlatform()).isEqualTo(Platform.ANDROID);
		assertThat(device.getPushToken()).isEqualTo("fcm-token-1");
		assertThat(device.getPushUpdatedAt()).isEqualTo(REGISTERED_AT);
		assertThat(device.getRefreshTokenHash()).isEqualTo(HASH);
		assertThat(device.getRevokedAt()).isNull();
	}

	@Test
	@DisplayName("Device.register는 pushToken이 없으면 pushToken · pushUpdatedAt을 둘 다 비워 둔다")
	void registerWithoutPushTokenLeavesPushTokenAndPushUpdatedAtEmpty() {
		Device device = Device.register(OwnerType.VEHICLE, 77L, Platform.VEHICLE, null, REGISTERED_AT, HASH,
				EXPIRES_AT);

		assertThat(device.getOwnerType()).isEqualTo(OwnerType.VEHICLE);
		assertThat(device.getOwnerId()).isEqualTo(77L);
		assertThat(device.getPlatform()).isEqualTo(Platform.VEHICLE);
		assertThat(device.getPushToken()).isNull();
		assertThat(device.getPushUpdatedAt()).isNull();
		assertThat(device.getRefreshTokenHash()).isEqualTo(HASH);
		assertThat(device.getRevokedAt()).isNull();
	}

	@Test
	@DisplayName("Device.isLive는 revokedAt이 비어 있으면 true, 채워지면 false를 돌려준다")
	void isLiveReturnsTrueOnlyWhileRevokedAtIsEmpty() {
		Device live = Device.restore(1L, OwnerType.USER, 31L, Platform.IOS, null, null, HASH, null, EXPIRES_AT);
		Device revoked = Device.restore(2L, OwnerType.USER, 31L, Platform.IOS, null, null, HASH,
				REGISTERED_AT.plusSeconds(60), EXPIRES_AT);

		assertThat(live.isLive()).isTrue();
		assertThat(revoked.isLive()).isFalse();
	}

	@Test
	@DisplayName("Device.revoke는 revokedAt을, Device.updatePushToken은 pushToken · pushUpdatedAt을, Device.rotateRefreshToken은 refreshTokenHash를 바꾼다")
	void revokeUpdatePushTokenAndRotateRefreshTokenChangeOnlyTheirOwnFields() {
		Instant pushUpdatedAt = REGISTERED_AT.plusSeconds(30);
		Instant revokedAt = REGISTERED_AT.plusSeconds(90);
		Device device = Device.restore(10L, OwnerType.USER, 31L, Platform.ANDROID, "fcm-old", REGISTERED_AT, HASH,
				null, EXPIRES_AT);

		device.updatePushToken("fcm-new", pushUpdatedAt);

		assertThat(device.getPushToken()).isEqualTo("fcm-new");
		assertThat(device.getPushUpdatedAt()).isEqualTo(pushUpdatedAt);
		assertThat(device.getRefreshTokenHash()).isEqualTo(HASH);
		assertThat(device.getRevokedAt()).isNull();

		device.rotateRefreshToken(NEW_HASH, EXPIRES_AT.plus(Duration.ofDays(1)));

		assertThat(device.getRefreshTokenHash()).isEqualTo(NEW_HASH);
		assertThat(device.getPushToken()).isEqualTo("fcm-new");
		assertThat(device.getPushUpdatedAt()).isEqualTo(pushUpdatedAt);
		assertThat(device.getRevokedAt()).isNull();

		device.revoke(revokedAt);

		assertThat(device.getRevokedAt()).isEqualTo(revokedAt);
		assertThat(device.isLive()).isFalse();
		assertThat(device.getRefreshTokenHash()).isEqualTo(NEW_HASH);
		assertThat(device.getPushToken()).isEqualTo("fcm-new");
		assertThat(device.getPushUpdatedAt()).isEqualTo(pushUpdatedAt);
		assertThat(device.getDeviceId()).isEqualTo(10L);
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.USER);
		assertThat(device.getOwnerId()).isEqualTo(31L);
		assertThat(device.getPlatform()).isEqualTo(Platform.ANDROID);
	}

	@Test
	@DisplayName("LinkCode · MemberInvite · Device는 restore로 저장된 값을 그대로 가진 객체를 만든다")
	void restoreKeepsStoredValuesForLinkCodeMemberInviteAndDevice() {
		Instant expiresAt = Instant.parse("2026-11-05T12:10:00Z");
		Instant usedAt = REGISTERED_AT.plusSeconds(60);
		Instant deliveredAt = REGISTERED_AT.plusSeconds(62);
		Instant pushUpdatedAt = REGISTERED_AT.plusSeconds(10);
		Instant revokedAt = REGISTERED_AT.plusSeconds(120);

		LinkCode linkCode = LinkCode.restore(5012L, 77L, 601L, false, "K7P2QX9M", LinkCodeStatus.USED, expiresAt, usedAt,
				LinkMode.NEW, deliveredAt);
		MemberInvite invite = MemberInvite.restore(901L, 31L, "invite-token-1", expiresAt, usedAt);
		Device device = Device.restore(4001L, OwnerType.VEHICLE, 77L, Platform.VEHICLE, "fcm-token-9", pushUpdatedAt,
				HASH, revokedAt, EXPIRES_AT);

		assertThat(linkCode.getCodeId()).isEqualTo(5012L);
		assertThat(linkCode.getVehicleId()).isEqualTo(77L);
		assertThat(linkCode.getIssuedDeviceId()).isEqualTo(601L);
		assertThat(linkCode.getCode()).isEqualTo("K7P2QX9M");
		assertThat(linkCode.getStatus()).isEqualTo(LinkCodeStatus.USED);
		assertThat(linkCode.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(linkCode.getUsedAt()).isEqualTo(usedAt);
		assertThat(linkCode.getMode()).isEqualTo(LinkMode.NEW);
		assertThat(linkCode.getTokenDeliveredAt()).isEqualTo(deliveredAt);

		assertThat(invite.getInviteId()).isEqualTo(901L);
		assertThat(invite.getUserId()).isEqualTo(31L);
		assertThat(invite.getToken()).isEqualTo("invite-token-1");
		assertThat(invite.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(invite.getUsedAt()).isEqualTo(usedAt);

		assertThat(device.getDeviceId()).isEqualTo(4001L);
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.VEHICLE);
		assertThat(device.getOwnerId()).isEqualTo(77L);
		assertThat(device.getPlatform()).isEqualTo(Platform.VEHICLE);
		assertThat(device.getPushToken()).isEqualTo("fcm-token-9");
		assertThat(device.getPushUpdatedAt()).isEqualTo(pushUpdatedAt);
		assertThat(device.getRefreshTokenHash()).isEqualTo(HASH);
		assertThat(device.getRevokedAt()).isEqualTo(revokedAt);
	}

	@Test
	@DisplayName("Device.restore는 비어 있던 pushToken · pushUpdatedAt · revokedAt을 비운 채로 만든다")
	void restoreKeepsNullFieldsNull() {
		Device device = Device.restore(4002L, OwnerType.USER, 31L, Platform.IOS, null, null, HASH, null,
				EXPIRES_AT);

		assertThat(device.getDeviceId()).isEqualTo(4002L);
		assertThat(device.getPushToken()).isNull();
		assertThat(device.getPushUpdatedAt()).isNull();
		assertThat(device.getRevokedAt()).isNull();
		assertThat(device.isLive()).isTrue();
	}

	@Test
	@DisplayName("Device.restore는 저장된 refreshTokenExpiresAt을 그대로 가진 객체를 만들고, 값이 없던 옛 행은 비운 채로 만든다")
	void restoreKeepsStoredRefreshTokenExpiresAt() {
		Instant expiresAt = REGISTERED_AT.plus(Duration.ofDays(14));

		Device device = Device.restore(4003L, OwnerType.USER, 31L, Platform.ANDROID, "fcm-token-3", REGISTERED_AT,
				HASH, null, expiresAt);
		Device legacy = Device.restore(4004L, OwnerType.USER, 31L, Platform.ANDROID, null, null, HASH, null, null);

		assertThat(device.getDeviceId()).isEqualTo(4003L);
		assertThat(device.getRefreshTokenHash()).isEqualTo(HASH);
		assertThat(device.getRefreshTokenExpiresAt()).isEqualTo(expiresAt);
		assertThat(device.getPushToken()).isEqualTo("fcm-token-3");
		assertThat(device.getRevokedAt()).isNull();
		assertThat(legacy.getRefreshTokenExpiresAt()).isNull();
	}

	@Test
	@DisplayName("Device.register는 받은 refreshTokenExpiresAt을 그대로 가진 객체를 만든다")
	void registerKeepsRefreshTokenExpiresAt() {
		Instant expiresAt = REGISTERED_AT.plus(Duration.ofDays(14));

		Device device = Device.register(OwnerType.VEHICLE, 77L, Platform.VEHICLE, null, REGISTERED_AT, HASH, expiresAt);

		assertThat(device.getDeviceId()).isNull();
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.VEHICLE);
		assertThat(device.getOwnerId()).isEqualTo(77L);
		assertThat(device.getRefreshTokenHash()).isEqualTo(HASH);
		assertThat(device.getRefreshTokenExpiresAt()).isEqualTo(expiresAt);
		assertThat(device.getRevokedAt()).isNull();
	}

	@Test
	@DisplayName("Device.rotateRefreshToken(hash, expiresAt)은 refreshTokenHash와 refreshTokenExpiresAt을 함께 바꾸고 다른 값은 그대로 둔다")
	void rotateRefreshTokenChangesHashAndExpiresAtTogether() {
		Instant oldExpiresAt = REGISTERED_AT.plus(Duration.ofDays(14));
		Instant newExpiresAt = REGISTERED_AT.plus(Duration.ofDays(20));
		Device device = Device.restore(4005L, OwnerType.USER, 31L, Platform.IOS, "fcm-token-5", REGISTERED_AT, HASH,
				null, oldExpiresAt);

		device.rotateRefreshToken(NEW_HASH, newExpiresAt);

		assertThat(device.getRefreshTokenHash()).isEqualTo(NEW_HASH);
		assertThat(device.getRefreshTokenExpiresAt()).isEqualTo(newExpiresAt);
		assertThat(device.getPushToken()).isEqualTo("fcm-token-5");
		assertThat(device.getPushUpdatedAt()).isEqualTo(REGISTERED_AT);
		assertThat(device.getRevokedAt()).isNull();
		assertThat(device.getDeviceId()).isEqualTo(4005L);
	}

	@Test
	@DisplayName("Device.isRefreshTokenExpired는 만료 시각 1초 전이면 false, 만료 시각과 같거나 지나면 true다")
	void isRefreshTokenExpiredIsTrueFromExpiresAtOnward() {
		Instant expiresAt = REGISTERED_AT.plus(Duration.ofDays(14));
		Device device = Device.restore(4006L, OwnerType.USER, 31L, Platform.ANDROID, null, null, HASH, null,
				expiresAt);

		assertThat(device.isRefreshTokenExpired(expiresAt.minusSeconds(1))).as("만료 1초 전").isFalse();
		assertThat(device.isRefreshTokenExpired(expiresAt)).as("만료 시각과 같은 순간").isTrue();
		assertThat(device.isRefreshTokenExpired(expiresAt.plusSeconds(1))).as("만료 1초 뒤").isTrue();
	}

	@Test
	@DisplayName("Device.isRefreshTokenExpired는 refreshTokenExpiresAt이 없으면 true다")
	void isRefreshTokenExpiredIsTrueWhenExpiresAtIsMissing() {
		Device legacy = Device.restore(4007L, OwnerType.USER, 31L, Platform.ANDROID, null, null, HASH, null, null);

		assertThat(legacy.isRefreshTokenExpired(REGISTERED_AT)).isTrue();
	}
}

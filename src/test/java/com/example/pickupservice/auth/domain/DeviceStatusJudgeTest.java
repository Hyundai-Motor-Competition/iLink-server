package com.example.pickupservice.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceStatusJudgeTest {

	private static final long USER_ID = 31L;
	private static final Instant NOW = Instant.parse("2026-11-05T12:05:00Z");
	private static final String REFRESH_HASH = "a".repeat(64);

	private static Device liveUserDevice(long deviceId) {
		return Device.restore(deviceId, OwnerType.USER, USER_ID, Platform.ANDROID, "fcm_live", NOW.minusSeconds(3600),
				REFRESH_HASH, null, NOW.plus(Duration.ofDays(14)));
	}

	private static Device revokedUserDevice(long deviceId) {
		return Device.restore(deviceId, OwnerType.USER, USER_ID, Platform.IOS, "fcm_old", NOW.minusSeconds(7200),
				REFRESH_HASH, NOW.minusSeconds(60), NOW.plus(Duration.ofDays(14)));
	}

	private static MemberInvite pendingInvite() {
		return MemberInvite.restore(901L, USER_ID, "inv_pending", NOW.plusSeconds(3600), null);
	}

	private static MemberInvite expiredInvite() {
		return MemberInvite.restore(902L, USER_ID, "inv_expired", NOW.minusSeconds(1), null);
	}

	private static MemberInvite usedInvite() {
		return MemberInvite.restore(903L, USER_ID, "inv_used", NOW.plusSeconds(3600), NOW.minusSeconds(600));
	}

	@Test
	@DisplayName("DeviceStatus 판정은 살아 있는 USER 기기가 있으면 CONNECTED를 돌려준다")
	void judgeReturnsConnectedWhenLiveUserDeviceExists() {
		assertThat(DeviceStatusJudge.judge(List.of(liveUserDevice(501L)), null, NOW))
				.isEqualTo(MemberDeviceStatus.CONNECTED);

		assertThat(DeviceStatusJudge.judge(List.of(revokedUserDevice(500L), liveUserDevice(501L)), pendingInvite(), NOW))
				.isEqualTo(MemberDeviceStatus.CONNECTED);
		assertThat(DeviceStatusJudge.judge(List.of(liveUserDevice(501L)), expiredInvite(), NOW))
				.isEqualTo(MemberDeviceStatus.CONNECTED);

		assertThat(DeviceStatusJudge.judge(List.of(revokedUserDevice(500L)), pendingInvite(), NOW))
				.isEqualTo(MemberDeviceStatus.INVITE_PENDING);
		assertThat(DeviceStatusJudge.judge(List.of(revokedUserDevice(500L)), null, NOW))
				.isEqualTo(MemberDeviceStatus.NONE);
	}

	@Test
	@DisplayName("DeviceStatus 판정은 기기가 없고 최근 초대가 사용 전 · 만료 전이면 INVITE_PENDING을 돌려준다")
	void judgeReturnsInvitePendingWhenNoDeviceAndLatestInviteUnusedAndNotExpired() {
		assertThat(DeviceStatusJudge.judge(List.of(), pendingInvite(), NOW))
				.isEqualTo(MemberDeviceStatus.INVITE_PENDING);

		MemberInvite justBeforeExpiry = MemberInvite.restore(904L, USER_ID, "inv_edge", NOW.plusMillis(1), null);
		assertThat(DeviceStatusJudge.judge(List.of(), justBeforeExpiry, NOW))
				.isEqualTo(MemberDeviceStatus.INVITE_PENDING);
	}

	@Test
	@DisplayName("DeviceStatus 판정은 기기가 없고 최근 초대가 만료됐으면 INVITE_EXPIRED를 돌려준다")
	void judgeReturnsInviteExpiredWhenNoDeviceAndLatestInviteExpired() {
		assertThat(DeviceStatusJudge.judge(List.of(), expiredInvite(), NOW))
				.isEqualTo(MemberDeviceStatus.INVITE_EXPIRED);

		MemberInvite expiresExactlyNow = MemberInvite.restore(905L, USER_ID, "inv_now", NOW, null);
		assertThat(DeviceStatusJudge.judge(List.of(), expiresExactlyNow, NOW))
				.isEqualTo(MemberDeviceStatus.INVITE_EXPIRED);
	}

	@Test
	@DisplayName("DeviceStatus 판정은 기기도 초대도 없으면 NONE을 돌려준다")
	void judgeReturnsNoneWhenNoDeviceAndNoInvite() {
		assertThat(DeviceStatusJudge.judge(List.of(), null, NOW))
				.isEqualTo(MemberDeviceStatus.NONE);
	}

	@Test
	@DisplayName("DeviceStatus 판정은 살아 있는 기기가 없고 최근 초대가 사용됐으면 NONE을 돌려준다")
	void judgeReturnsNoneWhenNoLiveDeviceAndLatestInviteUsed() {
		assertThat(DeviceStatusJudge.judge(List.of(revokedUserDevice(500L)), usedInvite(), NOW))
				.isEqualTo(MemberDeviceStatus.NONE);
		assertThat(DeviceStatusJudge.judge(List.of(), usedInvite(), NOW))
				.isEqualTo(MemberDeviceStatus.NONE);

		MemberInvite usedAndExpired = MemberInvite.restore(906L, USER_ID, "inv_used_expired", NOW.minusSeconds(1),
				NOW.minusSeconds(600));
		assertThat(DeviceStatusJudge.judge(List.of(), usedAndExpired, NOW))
				.isEqualTo(MemberDeviceStatus.NONE);
	}
}

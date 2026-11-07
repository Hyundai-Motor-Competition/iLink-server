package com.example.pickupservice.auth.domain;

import java.time.Instant;
import java.util.List;

public final class DeviceStatusJudge {

	private DeviceStatusJudge() {
	}

	public static MemberDeviceStatus judge(List<Device> devices, MemberInvite latestInvite, Instant now) {
		if (devices.stream().anyMatch(Device::isLive)) {
			return MemberDeviceStatus.CONNECTED;
		}
		if (latestInvite == null || latestInvite.isUsed()) {
			return MemberDeviceStatus.NONE;
		}
		if (latestInvite.isExpired(now)) {
			return MemberDeviceStatus.INVITE_EXPIRED;
		}
		return MemberDeviceStatus.INVITE_PENDING;
	}
}

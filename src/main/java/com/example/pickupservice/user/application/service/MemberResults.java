package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.User;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class MemberResults {

	private MemberResults() {
	}

	static List<MemberResult> withDeviceStatus(List<User> users, LoadMemberDeviceStatusPort deviceStatusPort) {
		Map<Long, DeviceStatus> deviceStatuses = deviceStatusPort.loadDeviceStatuses(
				users.stream().map(User::getUserId).toList());
		return users.stream()
				.sorted(Comparator.comparing(User::getUserId))
				.map(user -> toResult(user, deviceStatuses))
				.toList();
	}

	private static MemberResult toResult(User user, Map<Long, DeviceStatus> deviceStatuses) {
		DeviceStatus deviceStatus = deviceStatuses.getOrDefault(user.getUserId(), DeviceStatus.NONE);
		return new MemberResult(user.getUserId(), user.getRole(), user.getName(), user.getPhone(), deviceStatus);
	}
}

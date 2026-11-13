package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.User;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class MemberResults {

	private MemberResults() {
	}

	static List<MemberResult> withDeviceStatus(List<User> users, LoadMemberDeviceStatusPort deviceStatusPort) {
		return withDeviceStatus(users, deviceStatusPort, user -> null);
	}

	static List<MemberResult> withDeviceStatus(List<User> users, LoadMemberDeviceStatusPort deviceStatusPort,
			Function<User, String> profileImageUrlResolver) {
		Map<Long, DeviceStatus> deviceStatuses = deviceStatusPort.loadDeviceStatuses(
				users.stream().map(User::getUserId).toList());
		return users.stream()
				.sorted(Comparator.comparing(User::getUserId))
				.map(user -> toResult(user, deviceStatuses, profileImageUrlResolver.apply(user)))
				.toList();
	}

	private static MemberResult toResult(User user, Map<Long, DeviceStatus> deviceStatuses, String profileImageUrl) {
		DeviceStatus deviceStatus = deviceStatuses.getOrDefault(user.getUserId(), DeviceStatus.NONE);
		return new MemberResult(user.getUserId(), user.getRole(), user.getName(), user.getPhone(), deviceStatus,
				profileImageUrl, user.getGrade());
	}
}

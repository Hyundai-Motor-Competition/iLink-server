package com.example.pickupservice.user.adapter.out.auth;

import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.RevokeMemberDevicesPort;
import com.example.pickupservice.user.domain.DeviceStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// auth 구현 전 임시 동작: 모든 멤버를 NONE으로 보고 폐기는 하지 않는다
@Component
class AuthMemberDeviceAdapter implements LoadMemberDeviceStatusPort, RevokeMemberDevicesPort {

	@Override
	public Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds) {
		return userIds.stream()
				.distinct()
				.collect(Collectors.toMap(Function.identity(), userId -> DeviceStatus.NONE));
	}

	@Override
	public void revokeDevicesOf(long userId) {
	}
}

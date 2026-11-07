package com.example.pickupservice.user.adapter.out.auth;

import com.example.pickupservice.auth.application.port.in.GetMemberDeviceStatusUseCase;
import com.example.pickupservice.auth.application.port.in.RevokeMemberDevicesUseCase;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.RevokeMemberDevicesPort;
import com.example.pickupservice.user.domain.DeviceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class AuthMemberDeviceAdapter implements LoadMemberDeviceStatusPort, RevokeMemberDevicesPort {

	private final GetMemberDeviceStatusUseCase getMemberDeviceStatusUseCase;
	private final RevokeMemberDevicesUseCase revokeMemberDevicesUseCase;

	@Override
	public Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds) {
		return getMemberDeviceStatusUseCase.getDeviceStatuses(userIds).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> DeviceStatus.valueOf(entry.getValue().name())));
	}

	@Override
	public void revokeDevicesOf(long userId) {
		revokeMemberDevicesUseCase.revokeDevicesOf(userId);
	}
}

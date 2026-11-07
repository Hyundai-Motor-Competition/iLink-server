package com.example.pickupservice.schedule.adapter.out.auth;

import com.example.pickupservice.auth.application.port.in.GetMemberDeviceStatusUseCase;
import com.example.pickupservice.schedule.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ScheduleMemberDeviceAdapter implements LoadMemberDeviceStatusPort {

	private final GetMemberDeviceStatusUseCase getMemberDeviceStatusUseCase;

	@Override
	public Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return getMemberDeviceStatusUseCase.getDeviceStatuses(userIds).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> DeviceStatus.valueOf(entry.getValue().name())));
	}
}

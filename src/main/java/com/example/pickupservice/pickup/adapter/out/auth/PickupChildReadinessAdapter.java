package com.example.pickupservice.pickup.adapter.out.auth;

import com.example.pickupservice.auth.application.port.in.GetMemberDeviceStatusUseCase;
import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.pickup.application.port.out.LoadChildReadinessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class PickupChildReadinessAdapter implements LoadChildReadinessPort {

	private final GetMemberDeviceStatusUseCase getMemberDeviceStatusUseCase;

	@Override
	public Map<Long, MemberDeviceStatus> loadDeviceStatuses(List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return getMemberDeviceStatusUseCase.getDeviceStatuses(userIds);
	}

	@Override
	public boolean hasLocationConsent(long childUserId) {
		return true;
	}
}

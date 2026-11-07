package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.GetMemberDeviceStatusUseCase;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.domain.DeviceStatusJudge;
import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.auth.domain.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetMemberDeviceStatusService implements GetMemberDeviceStatusUseCase {

	private final LoadDevicePort loadDevicePort;
	private final LoadInvitePort loadInvitePort;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public Map<Long, MemberDeviceStatus> getDeviceStatuses(List<Long> userIds) {
		Instant now = clock.instant();
		Map<Long, MemberDeviceStatus> statuses = new LinkedHashMap<>();
		for (Long userId : userIds) {
			statuses.computeIfAbsent(userId, id -> DeviceStatusJudge.judge(
					loadDevicePort.loadLiveDevices(OwnerType.USER, id),
					loadInvitePort.loadLatestInvite(id).orElse(null),
					now));
		}
		return statuses;
	}
}

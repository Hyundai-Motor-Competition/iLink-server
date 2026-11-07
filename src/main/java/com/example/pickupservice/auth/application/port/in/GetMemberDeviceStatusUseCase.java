package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.MemberDeviceStatus;

import java.util.List;
import java.util.Map;

public interface GetMemberDeviceStatusUseCase {

	Map<Long, MemberDeviceStatus> getDeviceStatuses(List<Long> userIds);
}

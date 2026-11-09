package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.auth.domain.MemberDeviceStatus;

import java.util.List;
import java.util.Map;

public interface LoadChildReadinessPort {

	Map<Long, MemberDeviceStatus> loadDeviceStatuses(List<Long> userIds);

	boolean hasLocationConsent(long childUserId);

}

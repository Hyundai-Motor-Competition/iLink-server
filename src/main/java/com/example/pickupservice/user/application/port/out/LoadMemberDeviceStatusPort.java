package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.DeviceStatus;

import java.util.List;
import java.util.Map;

public interface LoadMemberDeviceStatusPort {

	Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds);
}

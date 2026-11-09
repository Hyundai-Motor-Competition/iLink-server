package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.domain.DeviceStatus;

import java.util.List;
import java.util.Map;

public interface LoadMemberDeviceStatusPort {

	Map<Long, DeviceStatus> loadDeviceStatuses(List<Long> userIds);
}

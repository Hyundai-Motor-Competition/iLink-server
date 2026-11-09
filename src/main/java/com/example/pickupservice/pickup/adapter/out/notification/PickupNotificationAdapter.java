package com.example.pickupservice.pickup.adapter.out.notification;

import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import org.springframework.stereotype.Component;

import java.util.List;

// notification 구현 전 임시 동작: 기록한 알림이 없다고 본다
@Component
class PickupNotificationAdapter implements RecordNotificationPort {

	@Override
	public List<Long> recordNotifications(NotificationRequest request) {
		return List.of();
	}
}

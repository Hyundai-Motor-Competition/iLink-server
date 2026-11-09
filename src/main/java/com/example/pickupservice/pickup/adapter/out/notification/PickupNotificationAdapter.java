package com.example.pickupservice.pickup.adapter.out.notification;

import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class PickupNotificationAdapter implements RecordNotificationPort {

	@Override
	public List<Long> recordNotifications(NotificationRequest request) {
		return List.of();
	}
}

package com.example.pickupservice.pickup.adapter.out.notification;

import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.notification.application.port.in.RecordNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class PickupNotificationAdapter implements RecordNotificationPort {

	private final RecordNotificationUseCase recordNotificationUseCase;

	@Override
	public List<Long> recordNotifications(NotificationRequest request) {
		return recordNotificationUseCase.record(new com.example.pickupservice.notification.application.port.in.NotificationRequest(
				request.sessionId(),
				request.callId(),
				request.type(),
				request.receiverUserIds()
		));
	}
}

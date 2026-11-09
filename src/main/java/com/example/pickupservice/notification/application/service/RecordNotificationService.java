package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.notification.application.port.in.NotificationRequest;
import com.example.pickupservice.notification.application.port.in.RecordNotificationUseCase;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordNotificationService implements RecordNotificationUseCase {

	private final LoadPushTargetPort loadPushTargetPort;
	private final SaveNotificationPort saveNotificationPort;

	@Override
	@Transactional
	public List<Long> record(NotificationRequest request) {
		List<Long> notificationIds = new ArrayList<>();
		for (PushTarget target : loadPushTargetPort.loadPushTargets(request.receiverUserIds())) {
			if (target.pushToken() == null || target.pushToken().isEmpty()) {
				continue;
			}
			Notification saved = saveNotificationPort.save(
					Notification.create(request.sessionId(), request.callId(), request.type(), target.deviceId()));
			notificationIds.add(saved.getNotificationId());
		}
		return notificationIds;
	}
}

package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import com.example.pickupservice.notification.domain.Notification;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

final class ResendDelivery {

	private ResendDelivery() {
	}

	static boolean send(Notification created, LoadPushTargetPort loadPushTargetPort, SendPushPort sendPushPort,
			SaveNotificationPort saveNotificationPort, CallProgressPort callProgressPort, Clock clock) {
		Optional<PushTarget> target = loadPushTargetPort.loadPushTarget(created.getDeviceId());
		if (target.isEmpty()) {
			return false;
		}
		Map<String, String> data = Map.of("notificationId", String.valueOf(created.getNotificationId()));
		PushMessage message = new PushMessage(target.get(), NotificationTexts.title(created.getType()),
				NotificationTexts.body(created.getType()), data);
		if (!sendPushPort.send(message)) {
			return false;
		}
		created.markSent(clock.instant());
		saveNotificationPort.save(created);
		if ("CALL".equals(created.getType()) && created.getCallId() != null) {
			callProgressPort.markCallSent(created.getCallId(), clock.instant());
		}
		return true;
	}
}

package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.notification.application.port.in.ResendUndeliveredUseCase;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import com.example.pickupservice.notification.domain.Notification;
import com.example.pickupservice.notification.domain.ResendPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResendUndeliveredService implements ResendUndeliveredUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final SendPushPort sendPushPort;
	private final LoadPushTargetPort loadPushTargetPort;
	private final CallProgressPort callProgressPort;
	private final Clock clock;

	@Override
	@Transactional
	public int resendUndelivered(Instant now) {
		int sent = 0;
		for (Notification latest : latestPerGroup(loadNotificationPort.loadResendTargets(now))) {
			int attempt = loadNotificationPort.countAttempts(latest);
			boolean callAcknowledged = "CALL".equals(latest.getType())
					&& callProgressPort.isCallAcknowledged(latest.getCallId());
			if (!ResendPolicy.shouldResend(latest, attempt, callAcknowledged)) {
				continue;
			}
			Notification created = saveNotificationPort.save(
					Notification.create(latest.getSessionId(), latest.getCallId(), latest.getType(), latest.getDeviceId()));
			Optional<PushTarget> target = loadPushTargetPort.loadPushTarget(created.getDeviceId());
			if (target.isEmpty()) {
				continue;
			}
			Map<String, String> data = Map.of("notificationId", String.valueOf(created.getNotificationId()));
			PushMessage message = new PushMessage(target.get(), NotificationTexts.title(created.getType()),
					NotificationTexts.body(created.getType()), data);
			if (!sendPushPort.send(message)) {
				continue;
			}
			created.markSent(clock.instant());
			saveNotificationPort.save(created);
			sent++;
		}
		return sent;
	}

	private static List<Notification> latestPerGroup(List<Notification> rows) {
		Map<GroupKey, Notification> latest = new LinkedHashMap<>();
		for (Notification row : rows) {
			latest.merge(GroupKey.of(row), row,
					(a, b) -> a.getNotificationId() >= b.getNotificationId() ? a : b);
		}
		return new ArrayList<>(latest.values());
	}

	private record GroupKey(long sessionId, Long callId, String type, long deviceId) {

		static GroupKey of(Notification notification) {
			return new GroupKey(notification.getSessionId(), notification.getCallId(), notification.getType(),
					notification.getDeviceId());
		}
	}
}

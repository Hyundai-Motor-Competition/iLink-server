package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.notification.application.port.in.ResendUndeliveredUseCase;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.LoadResendGuardPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import com.example.pickupservice.notification.domain.Notification;
import com.example.pickupservice.notification.domain.ResendPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResendUndeliveredService implements ResendUndeliveredUseCase {

	private static final int MAX_ATTEMPT = 3;

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final SendPushPort sendPushPort;
	private final LoadPushTargetPort loadPushTargetPort;
	private final CallProgressPort callProgressPort;
	private final LoadResendGuardPort loadResendGuardPort;
	private final Clock clock;

	@Override
	public int resendUndelivered(Instant now) {
		int sent = 0;
		Map<Long, List<Notification>> sessionRows = new HashMap<>();
		for (List<Notification> group : groupTargets(loadNotificationPort.loadResendTargets(now))) {
			List<Notification> targets = group.stream()
					.filter(row -> loadNotificationPort.countAttempts(row) < MAX_ATTEMPT)
					.toList();
			if (targets.isEmpty()) {
				continue;
			}
			Notification first = targets.get(0);
			GroupKey key = GroupKey.of(first);
			List<Notification> groupRows = new ArrayList<>(targets);
			for (Notification row : sessionRows.computeIfAbsent(first.getSessionId(),
					loadNotificationPort::loadBySession)) {
				if (key.equals(GroupKey.of(row)) && groupRows.stream()
						.noneMatch(t -> Objects.equals(t.getNotificationId(), row.getNotificationId()))) {
					groupRows.add(row);
				}
			}
			Notification latest = groupRows.stream()
					.max(Comparator.comparingLong(Notification::getNotificationId))
					.orElseThrow();
			Instant lastAttemptAt = ResendPolicy.lastAttemptAt(groupRows);
			if (lastAttemptAt != null && lastAttemptAt.isAfter(now.minusSeconds(60))) {
				continue;
			}
			boolean isCall = "CALL".equals(latest.getType());
			if (!isCall && groupRows.stream().anyMatch(row -> row.getDeliveredAt() != null)) {
				continue;
			}
			if (loadResendGuardPort.isSessionEnded(latest.getSessionId())
					|| (latest.getCallId() != null && loadResendGuardPort.isCallRevoked(latest.getCallId()))) {
				continue;
			}
			int attempt = loadNotificationPort.countAttempts(latest);
			boolean callAcknowledged = isCall && callProgressPort.isCallAcknowledged(latest.getCallId());
			if (!ResendPolicy.shouldResend(latest, attempt, callAcknowledged)) {
				continue;
			}
			Notification created = saveNotificationPort.save(
					Notification.create(latest.getSessionId(), latest.getCallId(), latest.getType(), latest.getDeviceId()));
			if (ResendDelivery.send(created, loadPushTargetPort, sendPushPort, saveNotificationPort, callProgressPort,
					clock)) {
				sent++;
			}
		}
		return sent;
	}

	private static Collection<List<Notification>> groupTargets(List<Notification> rows) {
		Map<GroupKey, List<Notification>> groups = new LinkedHashMap<>();
		for (Notification row : rows) {
			groups.computeIfAbsent(GroupKey.of(row), k -> new ArrayList<>()).add(row);
		}
		return groups.values();
	}

	private record GroupKey(long sessionId, Long callId, String type, long deviceId) {

		static GroupKey of(Notification notification) {
			return new GroupKey(notification.getSessionId(), notification.getCallId(), notification.getType(),
					notification.getDeviceId());
		}
	}
}

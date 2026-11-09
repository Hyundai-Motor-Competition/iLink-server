package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.notification.application.port.in.DispatchSessionEventUseCase;
import com.example.pickupservice.notification.application.port.in.SessionEventMessage;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.example.pickupservice.notification.domain.Notification;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DispatchSessionEventService implements DispatchSessionEventUseCase {

	private static final Set<String> SKIPPED_TYPES = Set.of("ETA_UPDATED", "VEHICLE_LOCATION");

	private static final Map<String, String> EVENT_TO_NOTIFICATION_TYPE = Map.of(
			"SESSION_STARTED", "SESSION_STARTED",
			"DEPARTURE_NOTICE_SENT", "DEPARTURE",
			"CALL_TRIGGERED", "CALL",
			"PREP_TIME_EXTENDED", "PREP_EXTENDED",
			"HELP_REQUESTED", "HELP");

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final SendPushPort sendPushPort;
	private final LoadPushTargetPort loadPushTargetPort;
	private final CallProgressPort callProgressPort;
	private final Clock clock;

	@Override
	public void dispatch(SessionEventMessage message) {
		if (SKIPPED_TYPES.contains(message.type())) {
			return;
		}
		String notificationType = EVENT_TO_NOTIFICATION_TYPE.get(message.type());
		if (notificationType != null) {
			dispatchRows(message, notificationType);
			return;
		}
		dispatchDataMessage(message);
	}

	private void dispatchRows(SessionEventMessage message, String notificationType) {
		Long callId = extractCallId(message.payload());
		String title = NotificationTexts.title(notificationType);
		String body = NotificationTexts.body(notificationType, extractHelpType(message.payload()));
		boolean anySent = false;
		for (Long notificationId : extractNotificationIds(message.payload())) {
			Notification row = loadNotificationPort.load(notificationId).orElse(null);
			if (row == null || row.getSentAt() != null) {
				continue;
			}
			Optional<PushTarget> target = loadPushTargetPort.loadPushTarget(row.getDeviceId());
			if (target.isEmpty() || target.get().platform() == Platform.VEHICLE) {
				continue;
			}
			Map<String, String> data = Map.of("notificationId", String.valueOf(row.getNotificationId()));
			if (sendPushPort.send(new PushMessage(target.get(), title, body, data))) {
				row.markSent(clock.instant());
				saveNotificationPort.save(row);
				anySent = true;
			}
		}
		if ("CALL".equals(notificationType) && anySent && callId != null) {
			callProgressPort.markCallSent(callId, clock.instant());
		}
	}

	private void dispatchDataMessage(SessionEventMessage message) {
		Map<String, String> data = new HashMap<>();
		data.put("eventId", String.valueOf(message.eventId()));
		data.put("seq", String.valueOf(message.seq()));
		data.put("stateVersion", String.valueOf(message.stateVersion()));
		data.put("occurredAt", message.occurredAt().toString());
		data.put("payload", JSON_MAPPER.writeValueAsString(message.payload()));
		for (PushTarget target : loadPushTargetPort.loadPushTargets(message.receiverUserIds())) {
			if (target.platform() == Platform.VEHICLE) {
				continue;
			}
			sendPushPort.send(new PushMessage(target, null, null, data));
		}
	}

	private static Long extractCallId(Map<String, Object> payload) {
		if (payload != null && payload.get("callId") instanceof Number number) {
			return number.longValue();
		}
		return null;
	}

	private static List<Long> extractNotificationIds(Map<String, Object> payload) {
		if (payload != null && payload.get("notificationIds") instanceof List<?> ids) {
			return ids.stream()
					.filter(Number.class::isInstance)
					.map(id -> ((Number) id).longValue())
					.toList();
		}
		return List.of();
	}

	private static String extractHelpType(Map<String, Object> payload) {
		if (payload != null && payload.get("type") instanceof String type) {
			return type;
		}
		return null;
	}
}

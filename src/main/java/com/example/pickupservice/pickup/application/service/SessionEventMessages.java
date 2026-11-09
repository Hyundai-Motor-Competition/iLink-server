package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.domain.SessionEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SessionEventMessages {

	private SessionEventMessages() {
	}

	static SessionEventMessage of(SessionEvent event, List<Long> receiverUserIds) {
		return new SessionEventMessage(event.getSessionId(), event.getEventId(), event.getSeq(),
				event.getType().name(), event.getStateVersion(), event.getOccurredAt(), event.getPayload(),
				receiverUserIds);
	}

	static SessionEventMessage of(SessionEvent event, List<Long> receiverUserIds, List<Long> notificationIds) {
		Map<String, Object> payload = new HashMap<>(event.getPayload());
		payload.put("notificationIds", notificationIds);
		return new SessionEventMessage(event.getSessionId(), event.getEventId(), event.getSeq(),
				event.getType().name(), event.getStateVersion(), event.getOccurredAt(), payload, receiverUserIds);
	}

}

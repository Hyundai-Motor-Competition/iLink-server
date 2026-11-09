package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.domain.SessionEvent;

import java.util.List;

final class SessionEventMessages {

	private SessionEventMessages() {
	}

	static SessionEventMessage of(SessionEvent event, List<Long> receiverUserIds) {
		return new SessionEventMessage(event.getSessionId(), event.getEventId(), event.getSeq(),
				event.getType().name(), event.getStateVersion(), event.getOccurredAt(), event.getPayload(),
				receiverUserIds);
	}

}

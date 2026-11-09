package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;

import java.util.List;
import java.util.Map;

public record SessionEventPageResponse(
		List<SessionEventResponse> items,
		boolean hasMore
) {

	public static SessionEventPageResponse from(SessionEventPage page) {
		return new SessionEventPageResponse(
				page.items().stream().map(SessionEventResponse::from).toList(),
				page.hasMore());
	}

	public record SessionEventResponse(
			long eventId,
			long seq,
			SessionEventType type,
			int stateVersion,
			String occurredAt,
			Map<String, Object> payload
	) {

		static SessionEventResponse from(SessionEvent event) {
			return new SessionEventResponse(
					event.getEventId(),
					event.getSeq(),
					event.getType(),
					event.getStateVersion(),
					TimeUtils.toKst(event.getOccurredAt()),
					event.getPayload());
		}
	}

}

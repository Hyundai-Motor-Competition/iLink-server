package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.sse.SessionStreamRegistry;
import com.example.pickupservice.pickup.application.port.in.GetSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.ListSessionEventsUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.domain.SessionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class PickupStreamController {

	private static final int REPLAY_PAGE_SIZE = 100;

	private final GetSessionUseCase getSessionUseCase;
	private final ListSessionEventsUseCase listSessionEventsUseCase;
	private final SessionStreamRegistry sessionStreamRegistry;

	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@GetMapping(value = "/v1/pickup-sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId) {
		getSessionUseCase.getSession(new SessionQuery(principal, sessionId));
		SseEmitter emitter = sessionStreamRegistry.register(sessionId);
		if (lastEventId != null) {
			replay(principal, sessionId, lastEventId, emitter);
		}
		return emitter;
	}

	private void replay(AuthPrincipal principal, long sessionId, long lastEventId, SseEmitter emitter) {
		long afterSeq = lastEventId;
		boolean hasMore = true;
		while (hasMore) {
			SessionEventPage page = listSessionEventsUseCase.listEvents(sessionId, afterSeq, REPLAY_PAGE_SIZE, principal);
			for (SessionEvent event : page.items()) {
				sessionStreamRegistry.sendTo(sessionId, emitter, event);
				afterSeq = event.getSeq();
			}
			hasMore = page.hasMore() && !page.items().isEmpty();
		}
	}

}

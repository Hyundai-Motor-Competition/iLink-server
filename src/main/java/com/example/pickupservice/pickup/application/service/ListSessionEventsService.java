package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.ListSessionEventsUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListSessionEventsService implements ListSessionEventsUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SessionEventPort sessionEventPort;

	@Override
	public SessionEventPage listEvents(long sessionId, long afterSeq, int limit, AuthPrincipal principal) {
		PickupSession session = loadSessionPort.load(sessionId).orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, principal);
		return sessionEventPort.loadAfter(sessionId, afterSeq, limit);
	}

}

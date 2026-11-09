package com.example.pickupservice.pickup.adapter.in.sse;

import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SessionEventStreamListener {

	private final SessionStreamRegistry sessionStreamRegistry;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSessionEvent(SessionEventMessage message) {
		sessionStreamRegistry.send(message);
	}

}

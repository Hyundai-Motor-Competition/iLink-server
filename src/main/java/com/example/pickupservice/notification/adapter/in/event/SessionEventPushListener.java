package com.example.pickupservice.notification.adapter.in.event;

import com.example.pickupservice.notification.application.port.in.DispatchSessionEventUseCase;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SessionEventPushListener {

	private final DispatchSessionEventUseCase dispatchSessionEventUseCase;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSessionEvent(SessionEventMessage message) {
		dispatchSessionEventUseCase.dispatch(new com.example.pickupservice.notification.application.port.in.SessionEventMessage(
				message.sessionId(),
				message.eventId(),
				message.seq(),
				message.type(),
				message.stateVersion(),
				message.occurredAt(),
				message.payload(),
				message.receiverUserIds()));
	}

}

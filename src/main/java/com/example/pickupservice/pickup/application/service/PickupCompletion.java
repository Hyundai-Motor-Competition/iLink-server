package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveOutcomePort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PickupCompletion {

	private static final String VEHICLE_STOPPED = SessionEventType.VEHICLE_STOPPED.name();
	private static final String SESSION_COMPLETED = SessionEventType.SESSION_COMPLETED.name();
	private static final String PREDICTED_STOP_AT = "predictedStopAt";

	private final SaveOutcomePort saveOutcomePort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;

	public void record(PickupSession completed, BoardingMethod method, Instant now, List<MemberInfo> members) {
		saveOutcomePort.saveOutcome(outcome(completed.getSessionId(), now, method));
		SessionEvent event = sessionEventPort.append(completed.getSessionId(), SESSION_COMPLETED,
				completed.getStateVersion(), now, Map.of());
		publishSessionEventPort.publish(
				SessionEventMessages.of(event, SessionReceivers.guardiansAndSessionChildren(completed, members)));
	}

	private PickupOutcome outcome(long sessionId, Instant now, BoardingMethod method) {
		Optional<SessionEvent> stopped = sessionEventPort.loadFirst(sessionId, VEHICLE_STOPPED);
		Instant actualStopAt = stopped.map(SessionEvent::getOccurredAt).orElse(null);
		Instant predictedStopAt = stopped
				.map(SessionEvent::getPayload)
				.map(payload -> payload.get(PREDICTED_STOP_AT))
				.map(value -> Instant.parse(String.valueOf(value)))
				.orElse(null);
		return PickupOutcome.complete(sessionId, now, method, predictedStopAt, actualStopAt);
	}

}

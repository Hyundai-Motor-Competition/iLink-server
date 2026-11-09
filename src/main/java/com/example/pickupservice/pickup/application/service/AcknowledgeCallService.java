package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallCommand;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallUseCase;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeResult;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AcknowledgeCallService implements AcknowledgeCallUseCase {

	private static final String CALL_ACKNOWLEDGED = SessionEventType.CALL_ACKNOWLEDGED.name();

	private final LoadCallPort loadCallPort;
	private final SaveCallPort saveCallPort;
	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final Clock clock;

	@Override
	public AcknowledgeResult acknowledge(AcknowledgeCallCommand command) {
		Call first = loadCallPort.loadCall(command.callId())
				.orElseThrow(PickupErrors::notFound);
		PickupSession locked = loadSessionPort.loadForUpdate(first.getSessionId())
				.orElseThrow(PickupErrors::notFound);
		Call call = loadCallPort.loadCall(command.callId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		if (call.getStatus() == CallStatus.REVOKED) {
			throw PickupErrors.callAlreadyRevoked();
		}
		Instant now = clock.instant();
		try {
			locked.acknowledgeChild(command.principal().id(), now);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		PickupSession session = saveSessionPort.save(locked);
		boolean alreadyAcknowledged = call.getStatus() == CallStatus.ACKNOWLEDGED;
		call.acknowledgeIfAllChildren(session.getChildren(), now);
		if (!alreadyAcknowledged && call.getStatus() == CallStatus.ACKNOWLEDGED) {
			saveCallPort.saveCall(call);
			SessionEvent event = sessionEventPort.append(session.getSessionId(), CALL_ACKNOWLEDGED,
					session.getStateVersion(), now, Map.of());
			publishSessionEventPort.publish(SessionEventMessages.of(event,
					SessionReceivers.guardians(loadFamilyPort.loadMembers(session.getFamilyId()))));
		}
		return new AcknowledgeResult(call.getCallId(), call.getStatus(), call.getAcknowledgedAt());
	}

}

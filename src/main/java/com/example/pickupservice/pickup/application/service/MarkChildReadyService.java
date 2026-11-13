package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.MarkChildReadyCommand;
import com.example.pickupservice.pickup.application.port.in.MarkChildReadyResult;
import com.example.pickupservice.pickup.application.port.in.MarkChildReadyUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class MarkChildReadyService implements MarkChildReadyUseCase {

	private static final String CHILD_READY = SessionEventType.CHILD_READY.name();

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final Clock clock;

	@Override
	public MarkChildReadyResult markReady(MarkChildReadyCommand command) {
		long childUserId = command.principal().id();
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		if (locked.getSessionStatus() == SessionStatus.COMPLETED
				|| locked.getSessionStatus() == SessionStatus.CANCELLED) {
			throw PickupErrors.sessionAlreadyFinished();
		}
		SessionChild child = locked.getChildren().stream()
				.filter(candidate -> candidate.getChildUserId() == childUserId)
				.findFirst()
				.orElseThrow(PickupErrors::notFound);
		if (child.getReadyAt() != null) {
			return new MarkChildReadyResult(childUserId, child.getReadyAt());
		}
		Instant now = clock.instant();
		try {
			locked.markChildReady(childUserId, now);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), CHILD_READY, session.getStateVersion(),
				now, Map.of("childUserId", childUserId));
		publishSessionEventPort.publish(SessionEventMessages.of(event,
				SessionReceivers.guardians(loadFamilyPort.loadMembers(session.getFamilyId()))));
		return new MarkChildReadyResult(childUserId, now);
	}

}

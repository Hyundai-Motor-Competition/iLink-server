package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.HelpRequestCommand;
import com.example.pickupservice.pickup.application.port.in.HelpRequestResult;
import com.example.pickupservice.pickup.application.port.in.RequestHelpUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.HelpRequestDecision;
import com.example.pickupservice.pickup.domain.HelpRequestRule;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class RequestHelpService implements RequestHelpUseCase {

	private static final String HELP_REQUESTED = SessionEventType.HELP_REQUESTED.name();
	private static final String HELP_NOTIFICATION = "HELP";

	private final LoadSessionPort loadSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final RecordNotificationPort recordNotificationPort;
	private final PickupProperties pickupProperties;
	private final Clock clock;

	@Override
	public HelpRequestResult request(HelpRequestCommand command) {
		PickupSession session = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, command.principal());
		if (session.isFinished()) {
			throw PickupErrors.sessionAlreadyFinished();
		}
		long childUserId = command.principal().id();
		Instant now = clock.instant();
		HelpRequestDecision decision = HelpRequestRule.decide(
				sessionEventPort.loadLastHelpRequest(session.getSessionId(), childUserId)
						.map(SessionEvent::getOccurredAt),
				now, pickupProperties.helpRequestInterval());
		if (!decision.allowed()) {
			throw PickupErrors.helpRequestTooSoon(decision.nextAllowedAt());
		}
		SessionEvent event = sessionEventPort.append(session.getSessionId(), HELP_REQUESTED,
				session.getStateVersion(), now, Map.of("childUserId", childUserId, "type", command.type()));
		List<Long> guardianIds = SessionReceivers.guardians(loadFamilyPort.loadMembers(session.getFamilyId()));
		List<Long> notificationIds = recordNotificationPort.recordNotifications(
				new NotificationRequest(session.getSessionId(), null, HELP_NOTIFICATION, guardianIds));
		publishSessionEventPort.publish(SessionEventMessages.of(event, guardianIds, notificationIds));
		return new HelpRequestResult(decision.nextAllowedAt());
	}

}

package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.ExtendPrepTimeCommand;
import com.example.pickupservice.pickup.application.port.in.ExtendPrepTimeUseCase;
import com.example.pickupservice.pickup.application.port.in.PrepExtensionResult;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// SIG-03 준비 시간 추가
@Service
@Transactional
@RequiredArgsConstructor
public class ExtendPrepTimeService implements ExtendPrepTimeUseCase {

	private static final String PREP_TIME_EXTENDED = SessionEventType.PREP_TIME_EXTENDED.name();
	private static final String PREP_EXTENDED_NOTIFICATION = "PREP_EXTENDED";

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadCallPort loadCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final RecordNotificationPort recordNotificationPort;
	private final PickupProperties pickupProperties;
	private final Clock clock;

	@Override
	public PrepExtensionResult extend(ExtendPrepTimeCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		if (loadCallPort.loadLiveCall(locked.getSessionId()).isPresent()) {
			throw PickupErrors.callAlreadySent();
		}
		long childUserId = command.principal().id();
		Instant now = clock.instant();
		extendChildPrep(locked, childUserId, command.minutes(), now);
		PickupSession session = saveSessionPort.save(locked);
		SessionChild child = childOf(session, childUserId);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), PREP_TIME_EXTENDED,
				session.getStateVersion(), now, Map.of(
						"childUserId", childUserId,
						"extraPrepMinutes", child.getExtraPrepMinutes(),
						"callNotBefore", child.getCallNotBefore()));
		List<Long> guardianIds = SessionReceivers.guardians(loadFamilyPort.loadMembers(session.getFamilyId()));
		publishSessionEventPort.publish(SessionEventMessages.of(event, guardianIds));
		recordNotificationPort.recordNotifications(
				new NotificationRequest(session.getSessionId(), null, PREP_EXTENDED_NOTIFICATION, guardianIds));
		return new PrepExtensionResult(child.getExtraPrepMinutes(), child.getCallNotBefore());
	}

	private void extendChildPrep(PickupSession session, long childUserId, int minutes, Instant now) {
		int limitMinutes = (int) pickupProperties.prepExtensionLimit().toMinutes();
		try {
			session.extendChildPrep(childUserId, minutes, now, limitMinutes);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
	}

	private static SessionChild childOf(PickupSession session, long childUserId) {
		return session.getChildren().stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.findFirst()
				.orElseThrow(PickupErrors::notFound);
	}

}

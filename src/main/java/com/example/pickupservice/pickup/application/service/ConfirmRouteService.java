package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.ConfirmRouteCommand;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.user.application.port.in.MemberInfo;
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
public class ConfirmRouteService implements ConfirmRouteUseCase {

	private static final String SPOT_CONFIRMED = SessionEventType.SPOT_CONFIRMED.name();
	private static final String DEPARTURE_NOTICE_SENT = SessionEventType.DEPARTURE_NOTICE_SENT.name();
	private static final String DEPARTURE_NOTIFICATION = "DEPARTURE";

	private final LoadSessionPort loadSessionPort;
	private final LoadSpotPort loadSpotPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final RecordNotificationPort recordNotificationPort;
	private final SessionAccess sessionAccess;
	private final Clock clock;

	@Override
	public SessionView confirmRoute(ConfirmRouteCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		if (locked.isFinished()) {
			throw PickupErrors.sessionAlreadyFinished();
		}
		requireSavedByFamily(command.spotId(), locked.getFamilyId());
		requireSessionSpot(locked, command);
		boolean firstConfirm = locked.getRouteVersion() == 0;
		locked.confirmRoute(command.totalMeters());
		PickupSession session = saveSessionPort.save(locked);
		Instant now = clock.instant();
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		publish(append(session, SPOT_CONFIRMED, now), SessionReceivers.guardiansAndSessionChildren(session, members));
		if (firstConfirm) {
			List<Long> notificationIds = recordNotificationPort.recordNotifications(
					new NotificationRequest(session.getSessionId(), null, DEPARTURE_NOTIFICATION,
							SessionReceivers.sessionChildren(session)));
			publishSessionEventPort.publish(SessionEventMessages.of(append(session, DEPARTURE_NOTICE_SENT, now),
					SessionReceivers.guardians(members), notificationIds));
		}
		return sessionAccess.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private void requireSavedByFamily(long spotId, long familyId) {
		boolean savedByFamily = loadSpotPort.loadSpot(spotId)
				.filter(spot -> spot.familyId() == familyId)
				.isPresent();
		if (!savedByFamily) {
			throw PickupErrors.spotNotSaved();
		}
	}

	private void requireSessionSpot(PickupSession locked, ConfirmRouteCommand command) {
		if (command.spotId() != locked.getSpotId()) {
			throw PickupErrors.stateVersionConflict(sessionAccess.latestView(locked, command.principal()));
		}
	}

	private SessionEvent append(PickupSession session, String type, Instant now) {
		return sessionEventPort.append(session.getSessionId(), type, session.getStateVersion(), now, Map.of());
	}

	private void publish(SessionEvent event, List<Long> receiverUserIds) {
		publishSessionEventPort.publish(SessionEventMessages.of(event, receiverUserIds));
	}

}

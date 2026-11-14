package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.ConfirmRouteCommand;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.in.SpotCandidate;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveTemporarySpotPort;
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

	private static final String SPOT_CHANGED = SessionEventType.SPOT_CHANGED.name();
	private static final String SPOT_CONFIRMED = SessionEventType.SPOT_CONFIRMED.name();

	private final LoadSessionPort loadSessionPort;
	private final LoadSpotPort loadSpotPort;
	private final SaveTemporarySpotPort saveTemporarySpotPort;
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
		Integer spotChangedStateVersion = null;
		if (command.candidate() != null) {
			changeToCandidate(locked, command.candidate());
			spotChangedStateVersion = locked.getStateVersion();
		} else {
			requireSavedByFamily(command.spotId(), locked.getFamilyId());
			requireSessionSpot(locked, command);
		}
		boolean firstConfirm = locked.getRouteVersion() == 0;
		locked.confirmRoute(command.totalMeters());
		PickupSession session = saveSessionPort.save(locked);
		Instant now = clock.instant();
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		List<Long> spotReceivers = SessionReceivers.guardiansAndSessionChildren(session, members);
		if (spotChangedStateVersion != null) {
			publish(append(session, SPOT_CHANGED, spotChangedStateVersion, now), spotReceivers);
		}
		publish(append(session, SPOT_CONFIRMED, session.getStateVersion(), now), spotReceivers);
		if (firstConfirm) {
			DepartureNotice.send(session, members, now, recordNotificationPort, sessionEventPort,
					publishSessionEventPort);
		}
		return sessionAccess.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private void changeToCandidate(PickupSession locked, SpotCandidate candidate) {
		SpotChangeGuard.requireSpotChangeable(locked, loadCallPort);
		locked.changeSpot(TemporarySpot.createAtCurrentAcademy(locked, candidate.name(), candidate.location(),
				loadSpotPort, saveTemporarySpotPort));
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

	private SessionEvent append(PickupSession session, String type, int stateVersion, Instant now) {
		return sessionEventPort.append(session.getSessionId(), type, stateVersion, now, Map.of());
	}

	private void publish(SessionEvent event, List<Long> receiverUserIds) {
		publishSessionEventPort.publish(SessionEventMessages.of(event, receiverUserIds));
	}

}

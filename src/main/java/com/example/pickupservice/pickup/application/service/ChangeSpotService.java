package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.ChangeSpotCommand;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.pickup.domain.SpotSnapshot;
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
public class ChangeSpotService implements ChangeSpotUseCase {

	private static final String SPOT_CHANGED = SessionEventType.SPOT_CHANGED.name();

	private final LoadSessionPort loadSessionPort;
	private final LoadSpotPort loadSpotPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final SessionAccess sessionAccess;
	private final Clock clock;

	@Override
	public SessionView changeSpot(ChangeSpotCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		SpotSnapshot spot = savedSpotOf(command.spotId(), locked.getFamilyId());
		changeSpot(locked, spot);
		if (loadCallPort.loadLiveCall(locked.getSessionId()).isPresent()) {
			throw PickupErrors.callAlreadySent();
		}
		PickupSession session = saveSessionPort.save(locked);
		Instant now = clock.instant();
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), SPOT_CHANGED,
				session.getStateVersion(), now, Map.of());
		publishSessionEventPort.publish(
				SessionEventMessages.of(event, SessionReceivers.guardiansAndSessionChildren(session, members)));
		return SessionViewAssembler.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private SpotSnapshot savedSpotOf(long spotId, long familyId) {
		return loadSpotPort.loadSpot(spotId)
				.filter(spot -> spot.familyId() == familyId)
				.map(spot -> new SpotSnapshot(spot.spotId(), spot.name(), spot.stopPoint(), spot.meetingPoint()))
				.orElseThrow(PickupErrors::spotNotSaved);
	}

	private static void changeSpot(PickupSession session, SpotSnapshot spot) {
		try {
			session.changeSpot(spot);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
	}

}

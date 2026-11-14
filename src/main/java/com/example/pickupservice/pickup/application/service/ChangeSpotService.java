package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotCommand;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.in.SpotCandidate;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveTemporarySpotPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.GuardId;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ChangeSpotService implements ChangeSpotUseCase {

	private static final String SPOT_CHANGED = SessionEventType.SPOT_CHANGED.name();
	private static final String SPOT_CONFIRMED = SessionEventType.SPOT_CONFIRMED.name();
	private static final String HERE_SPOT_NAME = "차량 정차 위치";
	private static final Set<GuardId> HERE_CONFIRMED_GUARDS = EnumSet.of(GuardId.G2, GuardId.G3);

	private final LoadSessionPort loadSessionPort;
	private final LoadSpotPort loadSpotPort;
	private final SaveTemporarySpotPort saveTemporarySpotPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final SessionAccess sessionAccess;
	private final CallEngine callEngine;
	private final Clock clock;

	@Override
	public SessionView changeSpot(ChangeSpotCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		if (locked.isFinished()) {
			throw PickupErrors.sessionAlreadyFinished();
		}
		if (locked.getSpotVersion() != command.spotVersion()) {
			throw PickupErrors.stateVersionConflict(sessionAccess.latestView(locked, command.principal()));
		}
		if (command.here()) {
			return callHere(locked, command);
		}
		SpotSnapshot spot;
		if (command.candidate() != null) {
			SpotChangeGuard.requireSpotChangeable(locked, loadCallPort);
			SpotCandidate candidate = command.candidate();
			spot = TemporarySpot.createAtCurrentAcademy(locked, candidate.name(), candidate.location(),
					loadSpotPort, saveTemporarySpotPort);
		} else {
			spot = savedSpotOf(command.spotId(), locked.getFamilyId());
			SpotChangeGuard.requireSpotChangeable(locked, loadCallPort);
		}
		locked.changeSpot(spot);
		PickupSession session = saveSessionPort.save(locked);
		Instant now = clock.instant();
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), SPOT_CHANGED,
				session.getStateVersion(), now, Map.of());
		publishSessionEventPort.publish(
				SessionEventMessages.of(event, SessionReceivers.guardiansAndSessionChildren(session, members)));
		return sessionAccess.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private SessionView callHere(PickupSession locked, ChangeSpotCommand command) {
		SpotChangeGuard.requireSpotChangeable(locked, loadCallPort);
		LatLng location = locked.getVehicleLocation();
		if (location == null) {
			throw BusinessException.invalidField("here", "받은 차량 위치가 없어 여기로 부를 수 없습니다");
		}
		Instant now = clock.instant();
		List<String> unmet = callEngine.evaluateGuard(locked, now).unmet().stream()
				.filter(guard -> !HERE_CONFIRMED_GUARDS.contains(guard))
				.map(GuardId::name)
				.toList();
		if (!unmet.isEmpty()) {
			throw PickupErrors.callGuardUnmet(unmet);
		}
		SpotSnapshot spot = TemporarySpot.createAtCurrentAcademy(locked, HERE_SPOT_NAME, location, loadSpotPort,
				saveTemporarySpotPort);
		if (locked.isCallHeld()) {
			locked.releaseCallHold();
		}
		locked.changeSpot(spot);
		int spotChangedStateVersion = locked.getStateVersion();
		boolean firstConfirm = locked.getRouteVersion() == 0;
		locked.confirmRoute(locked.getRouteTotalMeters());
		PickupSession session = saveSessionPort.save(locked);
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		List<Long> receivers = SessionReceivers.guardiansAndSessionChildren(session, members);
		publish(sessionEventPort.append(session.getSessionId(), SPOT_CHANGED, spotChangedStateVersion, now,
				Map.of()), receivers);
		publish(sessionEventPort.append(session.getSessionId(), SPOT_CONFIRMED, session.getStateVersion(), now,
				Map.of()), receivers);
		if (firstConfirm) {
			callEngine.sendDepartureNotice(session, members, now);
		}
		callEngine.createCall(session, CallTriggerType.MANUAL, now);
		return sessionAccess.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private void publish(SessionEvent event, List<Long> receiverUserIds) {
		publishSessionEventPort.publish(SessionEventMessages.of(event, receiverUserIds));
	}

	private SpotSnapshot savedSpotOf(long spotId, long familyId) {
		return loadSpotPort.loadSpot(spotId)
				.filter(spot -> spot.familyId() == familyId)
				.map(spot -> new SpotSnapshot(spot.spotId(), spot.name(), spot.stopPoint(), spot.meetingPoint()))
				.orElseThrow(PickupErrors::spotNotSaved);
	}

}

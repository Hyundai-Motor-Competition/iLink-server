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
import com.example.pickupservice.place.application.port.in.SpotInfo;
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
	// 여기로 부르기는 장소를 바꾸고 바로 확정하므로 G2(장소 확정) · G3(경로 일치)은 통과로 본다
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
			spot = temporarySpotOf(locked, command.candidate());
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

	// 여기로 부르기: 거절 검사를 모두 마친 뒤 차량 위치로 임시 장소를 만들고 바로 확정 · MANUAL 호출한다
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
			throw PickupErrors.callGuardUnmetConflict(unmet);
		}
		long academyId = currentAcademyId(locked);
		long spotId = saveTemporarySpotPort.saveTemporarySpot(locked.getFamilyId(), academyId, HERE_SPOT_NAME,
				location, location);
		locked.changeSpot(new SpotSnapshot(spotId, HERE_SPOT_NAME, location, location));
		int spotChangedStateVersion = locked.getStateVersion();
		locked.confirmRoute(locked.getRouteTotalMeters());
		PickupSession session = saveSessionPort.save(locked);
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		List<Long> receivers = SessionReceivers.guardiansAndSessionChildren(session, members);
		publish(sessionEventPort.append(session.getSessionId(), SPOT_CHANGED, spotChangedStateVersion, now,
				Map.of()), receivers);
		publish(sessionEventPort.append(session.getSessionId(), SPOT_CONFIRMED, session.getStateVersion(), now,
				Map.of()), receivers);
		callEngine.createCall(session, CallTriggerType.MANUAL, now);
		return sessionAccess.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private void publish(SessionEvent event, List<Long> receiverUserIds) {
		publishSessionEventPort.publish(SessionEventMessages.of(event, receiverUserIds));
	}

	private long currentAcademyId(PickupSession locked) {
		return loadSpotPort.loadSpot(locked.getSpotId())
				.map(SpotInfo::academyId)
				.orElseThrow(PickupErrors::spotNotSaved);
	}

	// 추천 장소 candidate: 거절 검사를 모두 마친 뒤에만 지금 장소의 학원으로 임시 장소를 만든다
	private SpotSnapshot temporarySpotOf(PickupSession locked, SpotCandidate candidate) {
		long academyId = currentAcademyId(locked);
		LatLng location = candidate.location();
		long spotId = saveTemporarySpotPort.saveTemporarySpot(locked.getFamilyId(), academyId, candidate.name(),
				location, location);
		return new SpotSnapshot(spotId, candidate.name(), location, location);
	}

	private SpotSnapshot savedSpotOf(long spotId, long familyId) {
		return loadSpotPort.loadSpot(spotId)
				.filter(spot -> spot.familyId() == familyId)
				.map(spot -> new SpotSnapshot(spot.spotId(), spot.name(), spot.stopPoint(), spot.meetingPoint()))
				.orElseThrow(PickupErrors::spotNotSaved);
	}

}

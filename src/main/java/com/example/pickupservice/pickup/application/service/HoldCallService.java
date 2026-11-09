package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.CallHoldCommand;
import com.example.pickupservice.pickup.application.port.in.HoldCallUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class HoldCallService implements HoldCallUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadCallPort loadCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionAccess sessionAccess;
	private final CallEngine callEngine;
	private final Clock clock;

	@Override
	public SessionView holdCall(CallHoldCommand command) {
		PickupSession locked = lockVisibleSession(command);
		if (loadCallPort.loadLiveCall(locked.getSessionId()).isPresent()) {
			throw PickupErrors.callAlreadySent();
		}
		try {
			locked.holdCall();
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		PickupSession session = saveSessionPort.save(locked);
		return view(session, command);
	}

	@Override
	public SessionView releaseCallHold(CallHoldCommand command) {
		PickupSession locked = lockVisibleSession(command);
		try {
			locked.releaseCallHold();
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		PickupSession session = saveSessionPort.save(locked);
		if (hasStoredEta(session)) {
			int navEtaSeconds = (int) Duration.between(session.getEtaUpdatedAt(), session.getEtaAt()).toSeconds();
			callEngine.triggerIfReady(session, navEtaSeconds, session.getRemainingMeters());
		}
		return view(session, command);
	}

	private PickupSession lockVisibleSession(CallHoldCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		return locked;
	}

	private static boolean hasStoredEta(PickupSession session) {
		return session.getEtaAt() != null && session.getRemainingMeters() != null
				&& session.getEtaUpdatedAt() != null;
	}

	private SessionView view(PickupSession session, CallHoldCommand command) {
		Instant now = clock.instant();
		long familyId = session.getFamilyId();
		return sessionAccess.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				loadFamilyPort.loadMembers(familyId), loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

}

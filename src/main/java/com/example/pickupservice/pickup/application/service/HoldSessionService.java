package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.HoldCommand;
import com.example.pickupservice.pickup.application.port.in.HoldSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Transactional
@RequiredArgsConstructor
public class HoldSessionService implements HoldSessionUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadCallPort loadCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionAccess sessionAccess;
	private final Clock clock;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final CallEngine callEngine;

	@Override
	public SessionView hold(HoldCommand command) {
		return change(command, PickupSession::hold, SessionEventType.SESSION_HELD);
	}

	@Override
	public SessionView releaseHold(HoldCommand command) {
		return change(command, session -> session.releaseHold(clock.instant()), SessionEventType.SESSION_RESUMED);
	}

	private SessionView change(HoldCommand command, Consumer<PickupSession> change, SessionEventType eventType) {
		AuthPrincipal principal = command.principal();
		PickupSession session = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, principal);
		sessionAccess.requireStateVersion(session, command.stateVersion(), principal);
		try {
			change.accept(session);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		PickupSession saved = saveSessionPort.save(session);
		Instant now = clock.instant();
		SessionEvent event = sessionEventPort.append(saved.getSessionId(), eventType.name(), saved.getStateVersion(),
				now, Map.of());
		long familyId = saved.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		publishSessionEventPort.publish(SessionEventMessages.of(event, SessionReceivers.guardians(members)));
		if (eventType == SessionEventType.SESSION_RESUMED && hasStoredEta(saved)) {
			int navEtaSeconds = (int) Duration.between(saved.getEtaUpdatedAt(), saved.getEtaAt()).toSeconds();
			callEngine.triggerIfReady(saved, navEtaSeconds, saved.getRemainingMeters());
		}
		return sessionAccess.assemble(saved, principal, loadFamilyPort.loadFamily(familyId), members,
				loadCallPort.loadLatestCall(saved.getSessionId()), now);
	}

	private static boolean hasStoredEta(PickupSession session) {
		return session.getEtaAt() != null && session.getRemainingMeters() != null
				&& session.getEtaUpdatedAt() != null;
	}

}

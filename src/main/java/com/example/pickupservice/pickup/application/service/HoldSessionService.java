package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.HoldCommand;
import com.example.pickupservice.pickup.application.port.in.HoldSessionUseCase;
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
import java.util.function.Consumer;

// SES-06 세션 보류(PUT) · 보류 해제(DELETE)
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

	@Override
	public SessionView hold(HoldCommand command) {
		return change(command, PickupSession::hold);
	}

	@Override
	public SessionView releaseHold(HoldCommand command) {
		return change(command, session -> session.releaseHold(clock.instant()));
	}

	private SessionView change(HoldCommand command, Consumer<PickupSession> change) {
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
		long familyId = saved.getFamilyId();
		return SessionViewAssembler.assemble(saved, principal, loadFamilyPort.loadFamily(familyId),
				loadFamilyPort.loadMembers(familyId), loadCallPort.loadLatestCall(saved.getSessionId()),
				clock.instant());
	}

}

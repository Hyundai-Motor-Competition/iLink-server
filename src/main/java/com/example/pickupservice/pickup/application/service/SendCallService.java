package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.CallResult;
import com.example.pickupservice.pickup.application.port.in.SendCallCommand;
import com.example.pickupservice.pickup.application.port.in.SendCallUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.GuardId;
import com.example.pickupservice.pickup.domain.GuardResult;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.user.domain.JoinCallMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class SendCallService implements SendCallUseCase {

	private final LoadSessionPort loadSessionPort;
	private final LoadCallPort loadCallPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionAccess sessionAccess;
	private final CallEngine callEngine;
	private final Clock clock;

	@Override
	public CallResult send(SendCallCommand command) {
		PickupSession session = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, command.principal());
		sessionAccess.requireStateVersion(session, command.stateVersion(), command.principal());
		long familyId = session.getFamilyId();
		if (loadFamilyPort.loadFamily(familyId).joinCallMode() == JoinCallMode.AUTO) {
			throw PickupErrors.notGuardianConfirmMode();
		}
		if (loadCallPort.loadLiveCall(session.getSessionId()).isPresent()) {
			throw PickupErrors.callAlreadySent();
		}
		Instant now = clock.instant();
		GuardResult guardResult = callEngine.evaluateGuard(session, now);
		if (!guardResult.met()) {
			throw PickupErrors.callGuardUnmet(guardResult.unmet().stream().map(GuardId::name).toList());
		}
		Call call = callEngine.createCall(session, CallTriggerType.MANUAL, now);
		return new CallResult(call.getCallId(), call.getStatus());
	}

}

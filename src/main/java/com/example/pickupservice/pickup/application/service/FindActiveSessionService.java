package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ActiveSessionResult;
import com.example.pickupservice.pickup.application.port.in.FindActiveSessionUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FindActiveSessionService implements FindActiveSessionUseCase {

	private final LoadSessionPort loadSessionPort;

	@Override
	public Optional<ActiveSessionResult> findActive(AuthPrincipal principal) {
		return activeSessionOf(principal)
				.map(session -> new ActiveSessionResult(session.getSessionId(), session.getSessionStatus(),
						session.getStateVersion()));
	}

	private Optional<PickupSession> activeSessionOf(AuthPrincipal principal) {
		if (principal.type() == PrincipalType.VEHICLE) {
			return loadSessionPort.loadActiveByVehicle(principal.id());
		}
		if (principal.role() == Role.CHILD) {
			return loadSessionPort.loadActiveByChild(principal.id());
		}
		return loadSessionPort.loadActiveByFamily(principal.familyId());
	}

}

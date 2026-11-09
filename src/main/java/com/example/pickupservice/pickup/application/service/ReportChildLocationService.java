package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.application.port.in.ChildLocationCommand;
import com.example.pickupservice.pickup.application.port.in.ReportChildLocationUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadChildReadinessPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// SIG-04 자녀 위치: 위치는 실시간 이벤트 19종에 없어 저장 · 발행하지 않고 stateVersion도 올리지 않는다
@Service
@Transactional
@RequiredArgsConstructor
public class ReportChildLocationService implements ReportChildLocationUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadChildReadinessPort loadChildReadinessPort;

	@Override
	public void report(ChildLocationCommand command) {
		AuthPrincipal principal = command.principal();
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, principal);
		if (!loadChildReadinessPort.hasLocationConsent(principal.id())) {
			throw PickupErrors.locationConsentRequired();
		}
		if (isFinished(locked)) {
			throw PickupErrors.sessionAlreadyFinished();
		}
		ChildPhase phase = phaseOf(locked, principal.id());
		if (phase != ChildPhase.CALLED && phase != ChildPhase.MEETING) {
			throw PickupErrors.childNotCalled();
		}
		updateLocation(locked, principal.id(), new LatLng(command.lat(), command.lng()), command.at());
		saveSessionPort.save(locked);
	}

	private static boolean isFinished(PickupSession session) {
		return session.getSessionStatus() == SessionStatus.COMPLETED
				|| session.getSessionStatus() == SessionStatus.CANCELLED;
	}

	private static ChildPhase phaseOf(PickupSession session, long childUserId) {
		return session.getChildren().stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.map(SessionChild::getChildPhase)
				.findFirst()
				.orElseThrow(PickupErrors::notFound);
	}

	private static void updateLocation(PickupSession session, long childUserId, LatLng location, Instant at) {
		try {
			session.updateChildLocation(childUserId, location, at);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
	}

}

package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Objects;

// 세션을 읽거나 바꾸는 서비스가 함께 쓰는 확인: 주체가 이 세션을 볼 수 있는지, 요청 stateVersion이 서버와 같은지
@Component
@RequiredArgsConstructor
public class SessionAccess {

	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final Clock clock;

	// 차량은 vehicleId, 사용자는 familyId, 자녀는 세션에 자기가 있는지로 본다. 볼 수 없으면 NOT_FOUND
	public static void requireVisible(PickupSession session, AuthPrincipal principal) {
		if (!isVisible(session, principal)) {
			throw PickupErrors.notFound();
		}
	}

	// 다르면 STATE_VERSION_CONFLICT와 viewer 기준 최신 세션 뷰
	public void requireStateVersion(PickupSession session, int stateVersion, AuthPrincipal viewer) {
		if (session.getStateVersion() == stateVersion) {
			return;
		}
		throw PickupErrors.stateVersionConflict(latestView(session, viewer));
	}

	// STATE_VERSION_CONFLICT에 실어 보내는 viewer 기준 최신 세션 뷰
	public SessionView latestView(PickupSession session, AuthPrincipal viewer) {
		long familyId = session.getFamilyId();
		return SessionViewAssembler.assemble(session, viewer, loadFamilyPort.loadFamily(familyId),
				loadFamilyPort.loadMembers(familyId), loadCallPort.loadLatestCall(session.getSessionId()),
				clock.instant());
	}

	private static boolean isVisible(PickupSession session, AuthPrincipal principal) {
		if (principal.type() == PrincipalType.VEHICLE) {
			return session.getVehicleId() == principal.id();
		}
		if (principal.role() == Role.CHILD) {
			return session.getChildren().stream()
					.anyMatch(child -> child.getChildUserId() == principal.id());
		}
		return Objects.equals(session.getFamilyId(), principal.familyId());
	}

}

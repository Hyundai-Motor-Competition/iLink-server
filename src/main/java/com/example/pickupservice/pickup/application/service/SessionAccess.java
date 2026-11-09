package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SessionAccess {

	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionViewAssembler sessionViewAssembler;
	private final Clock clock;

	public static void requireVisible(PickupSession session, AuthPrincipal principal) {
		if (!isVisible(session, principal)) {
			throw PickupErrors.notFound();
		}
	}

	public void requireStateVersion(PickupSession session, int stateVersion, AuthPrincipal viewer) {
		if (session.getStateVersion() == stateVersion) {
			return;
		}
		throw PickupErrors.stateVersionConflict(latestView(session, viewer));
	}

	public SessionView latestView(PickupSession session, AuthPrincipal viewer) {
		long familyId = session.getFamilyId();
		return assemble(session, viewer, loadFamilyPort.loadFamily(familyId), loadFamilyPort.loadMembers(familyId),
				loadCallPort.loadLatestCall(session.getSessionId()), clock.instant());
	}

	public SessionView assemble(PickupSession session, AuthPrincipal viewer, FamilyInfo family,
			List<MemberInfo> members, Optional<Call> latestCall, Instant now) {
		return sessionViewAssembler.assemble(session, viewer, family, members, latestCall, now);
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

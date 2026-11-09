package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CallGuardView;
import com.example.pickupservice.pickup.application.port.in.CallProgressView;
import com.example.pickupservice.pickup.application.port.in.ChildLocationView;
import com.example.pickupservice.pickup.application.port.in.SessionBoardingView;
import com.example.pickupservice.pickup.application.port.in.SessionCallView;
import com.example.pickupservice.pickup.application.port.in.SessionChildView;
import com.example.pickupservice.pickup.application.port.in.SessionEtaView;
import com.example.pickupservice.pickup.application.port.in.SessionRouteView;
import com.example.pickupservice.pickup.application.port.in.SessionSpotView;
import com.example.pickupservice.pickup.application.port.in.SessionStatusView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.Guard;
import com.example.pickupservice.pickup.domain.GuardResult;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SessionViewAssembler {

	private static final PickupProperties DEFAULTS = PickupProperties.defaults();
	private static final Duration ETA_VALID_FOR = DEFAULTS.etaValidFor();
	private static final Duration CALL_BEFORE_ETA = DEFAULTS.callTriggerEta();
	private static final String NO_CALL = "NONE";

	private SessionViewAssembler() {
	}

	public static SessionView assemble(PickupSession session, AuthPrincipal viewer, FamilyInfo family,
			List<MemberInfo> members, Optional<Call> latestCall, Instant now) {
		boolean childViewer = isChild(viewer);
		return new SessionView(
				session.getSessionId(),
				session.getStateVersion(),
				status(session, latestCall),
				childViewer && !isRouteConfirmed(session) ? null : spot(session),
				eta(session, now),
				call(session, family, latestCall, now),
				childViewer ? childrenForChild(session, members, viewer.id()) : children(session, members),
				new SessionRouteView(session.getRouteVersion(), session.getRemainingMeters()),
				boarding(session),
				null);
	}

	private static boolean isChild(AuthPrincipal viewer) {
		return viewer.type() == PrincipalType.USER && viewer.role() == Role.CHILD;
	}

	// 첫 경로 확정(SES-04) 때 자녀에게 출발 알림이 가므로, 그 뒤로는 장소를 다시 숨기지 않는다
	private static boolean isRouteConfirmed(PickupSession session) {
		return session.getRouteVersion() > 0;
	}

	private static SessionStatusView status(PickupSession session, Optional<Call> latestCall) {
		String callStatus = latestCall.map(call -> call.getStatus().name()).orElse(NO_CALL);
		return new SessionStatusView(session.getSessionStatus(), session.getVehiclePhase(), callStatus,
				session.getSpotStatus());
	}

	private static SessionSpotView spot(PickupSession session) {
		boolean locked = session.getChildren().stream()
				.anyMatch(child -> child.getChildPhase() == ChildPhase.MEETING);
		return new SessionSpotView(session.getSpotId(), session.getSpotName(), session.getSpotVersion(), locked);
	}

	private static SessionEtaView eta(PickupSession session, Instant now) {
		Instant etaAt = session.getEtaAt();
		Integer remainingSeconds = etaAt == null
				? null
				: (int) Math.max(0, Duration.between(now, etaAt).toSeconds());
		return new SessionEtaView(etaAt, remainingSeconds, session.getEtaUpdatedAt(),
				session.isEtaStale(now, ETA_VALID_FOR));
	}

	private static SessionCallView call(PickupSession session, FamilyInfo family, Optional<Call> latestCall,
			Instant now) {
		GuardResult guard = new Guard(ETA_VALID_FOR).evaluate(session, now);
		CallGuardView guardView = new CallGuardView(guard.met(),
				guard.unmet().stream().map(Enum::name).toList());
		CallProgressView progress = latestCall
				.map(call -> new CallProgressView(call.getAcceptedAt(), call.getSentAt(), call.getDeliveredAt(),
						call.getAcknowledgedAt()))
				.orElse(new CallProgressView(null, null, null, null));
		return new SessionCallView(family.joinCallMode(), latestCall.map(Call::getCallId).orElse(null),
				session.isCallHeld(), scheduledAt(session), guardView, progress);
	}

	private static Instant scheduledAt(PickupSession session) {
		if (session.getEtaAt() == null) {
			return null;
		}
		Instant beforeEta = session.getEtaAt().minus(CALL_BEFORE_ETA);
		return session.getChildren().stream()
				.map(SessionChild::getCallNotBefore)
				.filter(Objects::nonNull)
				.filter(beforeEta::isBefore)
				.max(Instant::compareTo)
				.orElse(beforeEta);
	}

	private static List<SessionChildView> children(PickupSession session, List<MemberInfo> members) {
		Map<Long, String> names = namesOf(members);
		return session.getChildren().stream()
				.map(child -> childView(child, names, location(child)))
				.toList();
	}

	// 자녀에게는 자기 항목만, 위치는 빼고 준다
	private static List<SessionChildView> childrenForChild(PickupSession session, List<MemberInfo> members,
			long childUserId) {
		Map<Long, String> names = namesOf(members);
		return session.getChildren().stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.map(child -> childView(child, names, null))
				.toList();
	}

	private static Map<Long, String> namesOf(List<MemberInfo> members) {
		return members.stream()
				.collect(Collectors.toMap(MemberInfo::userId, MemberInfo::name, (first, second) -> first));
	}

	private static SessionChildView childView(SessionChild child, Map<Long, String> names,
			ChildLocationView location) {
		return new SessionChildView(child.getChildUserId(), names.get(child.getChildUserId()), child.getChildPhase(),
				child.getExtraPrepMinutes(), child.getCallNotBefore(), child.getAcknowledgedAt(), child.getBoardedAt(),
				location);
	}

	private static ChildLocationView location(SessionChild child) {
		if (child.getLastLocation() == null) {
			return null;
		}
		return new ChildLocationView(child.getLastLocation().lat(), child.getLastLocation().lng(),
				child.getLastLocatedAt());
	}

	private static SessionBoardingView boarding(PickupSession session) {
		if (session.getBoardingDetectedAt() == null) {
			return null;
		}
		String door = session.getBoardingDoor() == null ? null : session.getBoardingDoor().name();
		return new SessionBoardingView(session.getBoardingDetectedAt(), session.getBoardingCancelableUntil(), door);
	}
}

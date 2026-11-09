package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.CancelSessionCommand;
import com.example.pickupservice.pickup.application.port.in.CancelSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CancelSessionService implements CancelSessionUseCase {

	private static final String CALL_REVOKED = SessionEventType.CALL_REVOKED.name();
	private static final String SESSION_CANCELLED = SessionEventType.SESSION_CANCELLED.name();

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SaveCallPort saveCallPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final SessionAccess sessionAccess;
	private final Clock clock;

	@Override
	public SessionView cancel(CancelSessionCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		Instant now = clock.instant();
		cancel(locked);
		Optional<Call> liveCall = loadCallPort.loadLiveCall(locked.getSessionId());
		liveCall.ifPresent(call -> revoke(call, command.reason(), now));
		locked.getChildren().forEach(SessionChild::clearLocation);
		PickupSession session = saveSessionPort.save(locked);
		List<SessionEvent> events = new ArrayList<>();
		if (liveCall.isPresent()) {
			events.add(append(session, CALL_REVOKED, now));
		}
		events.add(append(session, SESSION_CANCELLED, now));
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		List<Long> receiverUserIds = SessionReceivers.guardiansAndSessionChildren(session, members);
		events.forEach(event -> publishSessionEventPort.publish(SessionEventMessages.of(event, receiverUserIds)));
		return sessionAccess.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private static void cancel(PickupSession session) {
		try {
			session.cancel();
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
	}

	private void revoke(Call call, String reason, Instant now) {
		try {
			call.revoke(reason, now);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		saveCallPort.saveCall(call);
	}

	private SessionEvent append(PickupSession session, String type, Instant now) {
		return sessionEventPort.append(session.getSessionId(), type, session.getStateVersion(), now, Map.of());
	}

}

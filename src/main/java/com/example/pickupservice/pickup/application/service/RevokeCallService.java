package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.RevokeCallCommand;
import com.example.pickupservice.pickup.application.port.in.RevokeCallUseCase;
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
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// CALL-03 호출 철회
@Service
@Transactional
@RequiredArgsConstructor
public class RevokeCallService implements RevokeCallUseCase {

	private static final String CALL_REVOKED = SessionEventType.CALL_REVOKED.name();

	private final LoadCallPort loadCallPort;
	private final SaveCallPort saveCallPort;
	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final SessionAccess sessionAccess;
	private final Clock clock;

	@Override
	public SessionView revoke(RevokeCallCommand command) {
		Call first = loadCallPort.loadCall(command.callId())
				.orElseThrow(PickupErrors::notFound);
		PickupSession locked = loadSessionPort.loadForUpdate(first.getSessionId())
				.orElseThrow(PickupErrors::notFound);
		Call call = loadCallPort.loadCall(command.callId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		Instant now = clock.instant();
		try {
			call.revoke(command.reason(), now);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		locked.revertChildrenToWaiting();
		saveCallPort.saveCall(call);
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), CALL_REVOKED,
				session.getStateVersion(), now, Map.of());
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		publishSessionEventPort.publish(
				SessionEventMessages.of(event, SessionReceivers.guardiansAndSessionChildren(session, members)));
		return SessionViewAssembler.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

}

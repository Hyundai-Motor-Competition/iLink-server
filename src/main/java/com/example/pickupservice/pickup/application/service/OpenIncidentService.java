package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.OpenIncidentCommand;
import com.example.pickupservice.pickup.application.port.in.OpenIncidentUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.PublishSessionEventPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.IncidentType;
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

@Service
@Transactional
@RequiredArgsConstructor
public class OpenIncidentService implements OpenIncidentUseCase {

	private static final String INCIDENT_OPENED = SessionEventType.INCIDENT_OPENED.name();

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionEventPort sessionEventPort;
	private final PublishSessionEventPort publishSessionEventPort;
	private final SessionAccess sessionAccess;
	private final Clock clock;

	@Override
	public SessionView openIncident(OpenIncidentCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		IncidentType type = IncidentType.valueOf(command.type());
		Instant now = clock.instant();
		openIncident(locked, type, now);
		PickupSession session = saveSessionPort.save(locked);
		SessionEvent event = sessionEventPort.append(session.getSessionId(), INCIDENT_OPENED,
				session.getStateVersion(), now, Map.of("type", type.name(), "childUserId", command.childUserId()));
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		publishSessionEventPort.publish(
				SessionEventMessages.of(event, SessionReceivers.guardiansAndSessionChildren(session, members)));
		return SessionViewAssembler.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	private static void openIncident(PickupSession session, IncidentType type, Instant now) {
		try {
			session.openIncident(type, now);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
	}

}

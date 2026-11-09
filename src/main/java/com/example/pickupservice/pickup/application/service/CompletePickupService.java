package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.CompletePickupCommand;
import com.example.pickupservice.pickup.application.port.in.CompletePickupUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

// SES-11 탑승 완료(IMMEDIATE · MANUAL)
@Service
@Transactional
@RequiredArgsConstructor
public class CompletePickupService implements CompletePickupUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionAccess sessionAccess;
	private final PickupCompletion pickupCompletion;
	private final Clock clock;

	@Override
	public SessionView complete(CompletePickupCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		Instant now = clock.instant();
		BoardingMethod method = BoardingMethod.valueOf(command.mode());
		List<Long> notBoardedChildUserIds = command.notBoardedChildUserIds() == null
				? List.of() : command.notBoardedChildUserIds();
		try {
			locked.complete(method, notBoardedChildUserIds, now);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		PickupSession session = saveSessionPort.save(locked);
		long familyId = session.getFamilyId();
		List<MemberInfo> members = loadFamilyPort.loadMembers(familyId);
		pickupCompletion.record(session, method, now, members);
		return SessionViewAssembler.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				members, loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

}

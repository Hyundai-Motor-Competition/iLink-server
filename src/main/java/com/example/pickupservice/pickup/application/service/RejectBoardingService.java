package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.pickup.application.port.in.RejectBoardingCommand;
import com.example.pickupservice.pickup.application.port.in.RejectBoardingUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RejectBoardingService implements RejectBoardingUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionAccess sessionAccess;
	private final Clock clock;

	@Override
	public SessionView rejectBoarding(RejectBoardingCommand command) {
		PickupSession locked = loadSessionPort.loadForUpdate(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(locked, command.principal());
		sessionAccess.requireStateVersion(locked, command.stateVersion(), command.principal());
		long childUserId = targetChildOf(locked, command.childUserId());
		Instant now = clock.instant();
		rejectBoarding(locked, childUserId, now);
		PickupSession session = saveSessionPort.save(locked);
		long familyId = session.getFamilyId();
		return SessionViewAssembler.assemble(session, command.principal(), loadFamilyPort.loadFamily(familyId),
				loadFamilyPort.loadMembers(familyId), loadCallPort.loadLatestCall(session.getSessionId()), now);
	}

	// childUserId가 없으면 세션 자녀가 1명일 때만 그 자녀로 본다
	private static long targetChildOf(PickupSession session, Long childUserId) {
		if (childUserId != null) {
			return childUserId;
		}
		List<SessionChild> children = session.getChildren();
		if (children.size() != 1) {
			throw BusinessException.invalidField("childUserId", "자녀가 여러 명이면 childUserId가 필요합니다");
		}
		return children.get(0).getChildUserId();
	}

	private static void rejectBoarding(PickupSession session, long childUserId, Instant now) {
		try {
			session.rejectBoarding(childUserId, now);
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
	}

}

package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.GetSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetSessionService implements GetSessionUseCase {

	private final LoadSessionPort loadSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadCallPort loadCallPort;
	private final SessionViewAssembler sessionViewAssembler;
	private final Clock clock;

	@Override
	public SessionView getSession(SessionQuery query) {
		PickupSession session = loadSessionPort.load(query.sessionId()).orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, query.principal());
		long familyId = session.getFamilyId();
		return sessionViewAssembler.assemble(session, query.principal(), loadFamilyPort.loadFamily(familyId),
				membersWithSessionChildren(session, loadFamilyPort.loadMembers(familyId)),
				loadCallPort.loadLatestCall(session.getSessionId()), clock.instant());
	}

	private List<MemberInfo> membersWithSessionChildren(PickupSession session, List<MemberInfo> members) {
		Set<Long> memberUserIds = members.stream().map(MemberInfo::userId).collect(Collectors.toSet());
		List<MemberInfo> removedChildren = session.getChildren().stream()
				.map(SessionChild::getChildUserId)
				.filter(childUserId -> !memberUserIds.contains(childUserId))
				.map(loadFamilyPort::loadMember)
				.flatMap(Optional::stream)
				.toList();
		if (removedChildren.isEmpty()) {
			return members;
		}
		return Stream.concat(members.stream(), removedChildren.stream()).toList();
	}

}

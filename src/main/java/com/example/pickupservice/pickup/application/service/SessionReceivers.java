package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.user.application.port.in.MemberInfo;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class SessionReceivers {

	private static final Set<Role> GUARDIAN_ROLES = EnumSet.of(Role.GUARDIAN, Role.CO_GUARDIAN, Role.DELEGATE);

	private SessionReceivers() {
	}

	static List<Long> guardians(List<MemberInfo> members) {
		return members.stream()
				.filter(member -> GUARDIAN_ROLES.contains(member.role()))
				.map(MemberInfo::userId)
				.toList();
	}

	static List<Long> sessionChildren(PickupSession session) {
		return session.getChildren().stream()
				.map(SessionChild::getChildUserId)
				.toList();
	}

	static List<Long> guardiansAndSessionChildren(PickupSession session, List<MemberInfo> members) {
		return Stream.concat(guardians(members).stream(), sessionChildren(session).stream()).toList();
	}

}

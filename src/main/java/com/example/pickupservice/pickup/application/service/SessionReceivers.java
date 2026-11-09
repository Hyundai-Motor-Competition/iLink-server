package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.user.application.port.in.MemberInfo;

import java.util.List;
import java.util.stream.Stream;

final class SessionReceivers {

	private SessionReceivers() {
	}

	static List<Long> guardians(List<MemberInfo> members) {
		return members.stream()
				.filter(member -> member.role() == Role.GUARDIAN)
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

package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.out.LoadUserPort;

final class NonChildMemberLimit {

	private static final long MAX_NON_CHILD_MEMBERS = 5;

	private NonChildMemberLimit() {
	}

	static void checkRoomFor(LoadUserPort loadUserPort, long familyId) {
		if (loadUserPort.countNonChildMembers(familyId) >= MAX_NON_CHILD_MEMBERS) {
			throw UserErrors.memberLimitExceeded();
		}
	}
}

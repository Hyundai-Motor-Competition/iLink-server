package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;

final class FamilyChildGuard {

	private FamilyChildGuard() {
	}

	static MemberInfo requireChildOfFamily(LoadMemberUseCase loadMemberUseCase, long familyId, long childId) {
		return loadMemberUseCase.findMember(childId)
				.filter(member -> member.familyId() == familyId)
				.filter(member -> member.role() == Role.CHILD)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
	}
}

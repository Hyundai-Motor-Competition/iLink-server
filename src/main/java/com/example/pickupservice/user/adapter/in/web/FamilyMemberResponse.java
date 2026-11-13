package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.domain.DeviceStatus;

public record FamilyMemberResponse(
		long userId,
		Role role,
		String name,
		String phone,
		DeviceStatus deviceStatus,
		String profileImageUrl
) {

	public static FamilyMemberResponse from(MemberResult member) {
		return new FamilyMemberResponse(member.userId(), member.role(), member.name(), member.phone(),
				member.deviceStatus(), member.profileImageUrl());
	}

}

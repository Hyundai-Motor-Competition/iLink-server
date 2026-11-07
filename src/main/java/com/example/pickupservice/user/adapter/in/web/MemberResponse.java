package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.domain.DeviceStatus;

public record MemberResponse(
		long userId,
		Role role,
		String name,
		String phone,
		DeviceStatus deviceStatus
) {

	public static MemberResponse from(MemberResult member) {
		return new MemberResponse(member.userId(), member.role(), member.name(), member.phone(), member.deviceStatus());
	}

}

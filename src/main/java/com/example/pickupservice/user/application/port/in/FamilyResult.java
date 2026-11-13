package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.user.domain.JoinCallMode;

import java.util.List;

public record FamilyResult(
		long familyId,
		JoinCallMode joinCallMode,
		VehicleSummary vehicle,
		List<MemberResult> members,
		boolean childLocationEnabled
) {

	public FamilyResult(long familyId, JoinCallMode joinCallMode, VehicleSummary vehicle, List<MemberResult> members) {
		this(familyId, joinCallMode, vehicle, members, false);
	}

}

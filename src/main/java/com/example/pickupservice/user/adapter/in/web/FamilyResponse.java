package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.user.application.port.in.FamilyResult;
import com.example.pickupservice.user.domain.JoinCallMode;

import java.util.List;

public record FamilyResponse(
		long familyId,
		JoinCallMode joinCallMode,
		VehicleResponse vehicle,
		List<MemberResponse> members,
		boolean childLocationEnabled
) {

	public static FamilyResponse from(FamilyResult result) {
		return new FamilyResponse(
				result.familyId(),
				result.joinCallMode(),
				VehicleResponse.from(result.vehicle()),
				result.members().stream().map(MemberResponse::from).toList(),
				result.childLocationEnabled());
	}

}

package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.VehicleChild;
import com.example.pickupservice.schedule.application.port.in.VehicleSettingsSummary;
import com.example.pickupservice.user.domain.JoinCallMode;

import java.util.List;

public record VehicleSettingsSummaryResponse(
		String guardianNameMasked,
		List<ChildResponse> children,
		JoinCallMode joinCallMode
) {

	public static VehicleSettingsSummaryResponse from(VehicleSettingsSummary summary) {
		return new VehicleSettingsSummaryResponse(
				summary.guardianNameMasked(),
				summary.children().stream().map(ChildResponse::from).toList(),
				summary.joinCallMode());
	}

	public record ChildResponse(long userId, String name) {

		static ChildResponse from(VehicleChild child) {
			return new ChildResponse(child.userId(), child.name());
		}
	}
}

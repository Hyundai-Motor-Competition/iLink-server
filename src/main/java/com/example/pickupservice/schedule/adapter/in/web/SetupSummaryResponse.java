package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.SetupChild;
import com.example.pickupservice.schedule.application.port.in.SetupSchedule;
import com.example.pickupservice.schedule.application.port.in.SetupSpot;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.schedule.domain.JoinCallMode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SetupSummaryResponse(
		ChildResponse child,
		List<ScheduleSummaryResponse> schedules,
		List<PickupSpotResponse> pickupSpots,
		JoinCallMode joinCallMode,
		List<MissingSetupItem> missing,
		int settingsVersion
) {

	public static SetupSummaryResponse from(SetupSummary summary) {
		return new SetupSummaryResponse(
				ChildResponse.from(summary.child()),
				summary.schedules().stream().map(ScheduleSummaryResponse::from).toList(),
				summary.pickupSpots().stream().map(PickupSpotResponse::from).toList(),
				summary.joinCallMode(),
				summary.missing(),
				summary.settingsVersion());
	}

	public record ChildResponse(long userId, String name, DeviceStatus deviceStatus) {

		static ChildResponse from(SetupChild child) {
			return new ChildResponse(child.userId(), child.name(), child.deviceStatus());
		}
	}

	public record ScheduleSummaryResponse(long scheduleId, String academyName, ScheduleStatus status) {

		static ScheduleSummaryResponse from(SetupSchedule schedule) {
			return new ScheduleSummaryResponse(schedule.scheduleId(), schedule.academyName(), schedule.status());
		}
	}

	public record PickupSpotResponse(long spotId, String name, @JsonProperty("isDefault") boolean isDefault) {

		static PickupSpotResponse from(SetupSpot spot) {
			return new PickupSpotResponse(spot.spotId(), spot.name(), spot.isDefault());
		}
	}
}

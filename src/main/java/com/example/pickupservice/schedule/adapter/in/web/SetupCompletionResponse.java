package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.SetupCompletionResult;

public record SetupCompletionResponse(
		boolean syncedToVehicle
) {

	public static SetupCompletionResponse from(SetupCompletionResult result) {
		return new SetupCompletionResponse(result.syncedToVehicle());
	}
}

package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.user.application.port.in.ChangeCallModeCommand;
import com.example.pickupservice.user.domain.JoinCallMode;
import jakarta.validation.constraints.NotNull;

public record ChangeCallModeRequest(
		@NotNull JoinCallMode joinCallMode
) {

	public ChangeCallModeCommand toCommand(long familyId) {
		return new ChangeCallModeCommand(familyId, joinCallMode);
	}

}

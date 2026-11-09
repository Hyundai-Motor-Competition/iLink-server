package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.ActiveSessionResult;
import com.example.pickupservice.pickup.domain.SessionStatus;

public record ActiveSessionResponse(
		long sessionId,
		SessionStatus status,
		int stateVersion
) {

	public static ActiveSessionResponse from(ActiveSessionResult result) {
		return new ActiveSessionResponse(result.sessionId(), result.status(), result.stateVersion());
	}

}

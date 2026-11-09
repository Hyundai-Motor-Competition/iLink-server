package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.CallResult;
import com.example.pickupservice.pickup.domain.CallStatus;

public record SendCallResponse(
		long callId,
		CallStatus status
) {

	public static SendCallResponse from(CallResult result) {
		return new SendCallResponse(result.callId(), result.status());
	}

}

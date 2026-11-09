package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.AcknowledgeResult;
import com.example.pickupservice.pickup.domain.CallStatus;

import static com.example.pickupservice.pickup.adapter.in.web.KstTime.kst;

public record AcknowledgeCallResponse(
		long callId,
		CallStatus status,
		String acknowledgedAt
) {

	public static AcknowledgeCallResponse from(AcknowledgeResult result) {
		return new AcknowledgeCallResponse(result.callId(), result.status(), kst(result.acknowledgedAt()));
	}

}

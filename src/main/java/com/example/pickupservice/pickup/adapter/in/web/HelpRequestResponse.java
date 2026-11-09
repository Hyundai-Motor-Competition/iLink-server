package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.HelpRequestResult;

public record HelpRequestResponse(
		String nextAllowedAt
) {

	public static HelpRequestResponse from(HelpRequestResult result) {
		return new HelpRequestResponse(TimeUtils.toKst(result.nextAllowedAt()));
	}

}

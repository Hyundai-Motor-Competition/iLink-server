package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.PrepExtensionResult;

public record PrepExtensionResponse(
		int extraPrepMinutes,
		String callNotBefore
) {

	public static PrepExtensionResponse from(PrepExtensionResult result) {
		return new PrepExtensionResponse(result.extraPrepMinutes(), TimeUtils.toKst(result.callNotBefore()));
	}

}

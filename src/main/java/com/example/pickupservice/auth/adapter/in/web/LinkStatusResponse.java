package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.LinkStatus;
import com.example.pickupservice.auth.application.port.in.LinkStatusResult;

public record LinkStatusResponse(
		LinkStatus status,
		Long vehicleId,
		Long familyId,
		String accessToken,
		String refreshToken,
		Integer pollAfterSeconds
) {

	public static LinkStatusResponse from(LinkStatusResult result) {
		return new LinkStatusResponse(
				result.status(),
				result.vehicleId(),
				result.familyId(),
				result.accessToken(),
				result.refreshToken(),
				result.pollAfterSeconds());
	}

}

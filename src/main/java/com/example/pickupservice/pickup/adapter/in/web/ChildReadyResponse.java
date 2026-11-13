package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.MarkChildReadyResult;

import static com.example.pickupservice.pickup.adapter.in.web.KstTime.kst;

public record ChildReadyResponse(
		long childUserId,
		String readyAt
) {

	public static ChildReadyResponse from(MarkChildReadyResult result) {
		return new ChildReadyResponse(result.childUserId(), kst(result.readyAt()));
	}

}

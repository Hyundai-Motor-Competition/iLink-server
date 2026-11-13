package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.schedule.application.port.in.NextPickup;

public record NextPickupResponse(
		String academyName,
		String spotName,
		String targetAt
) {

	public static NextPickupResponse from(NextPickup nextPickup) {
		return new NextPickupResponse(nextPickup.academyName(), nextPickup.spotName(),
				TimeUtils.toKst(nextPickup.targetAt()));
	}
}

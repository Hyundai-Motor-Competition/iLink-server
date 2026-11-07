package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.LinkCodeInfo;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.common.time.TimeUtils;

public record LinkCodeInfoResponse(
		LinkMode status,
		String vehicleModel,
		String plateNumberMasked,
		String expiresAt
) {

	public static LinkCodeInfoResponse from(LinkCodeInfo info) {
		return new LinkCodeInfoResponse(
				info.status(),
				info.vehicleModel(),
				info.plateNumberMasked(),
				TimeUtils.toKst(info.expiresAt()));
	}

}

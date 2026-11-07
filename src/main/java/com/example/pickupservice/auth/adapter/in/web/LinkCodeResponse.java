package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.LinkCodeResult;
import com.example.pickupservice.common.time.TimeUtils;

public record LinkCodeResponse(
		long codeId,
		String code,
		String qrPayload,
		String expiresAt,
		int reissuesLeft
) {

	public static LinkCodeResponse from(LinkCodeResult result) {
		return new LinkCodeResponse(
				result.codeId(),
				result.code(),
				result.qrPayload(),
				TimeUtils.toKst(result.expiresAt()),
				result.reissuesLeft());
	}

}

package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.AuthPrincipalAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

final class CurrentDevice {

	private static final String DEVICE_ID_CLAIM = "deviceId";

	private CurrentDevice() {
	}

	static long id() {
		if (SecurityContextHolder.getContext().getAuthentication() instanceof AuthPrincipalAuthenticationToken token
				&& token.getCredentials().getClaim(DEVICE_ID_CLAIM) instanceof Number deviceId) {
			return deviceId.longValue();
		}
		throw BusinessException.of(ErrorCode.UNAUTHORIZED);
	}

}

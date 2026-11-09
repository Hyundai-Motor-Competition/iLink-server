package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;

public final class NotificationErrors {

	private NotificationErrors() {
	}

	public static BusinessException sessionAlreadyFinished() {
		return BusinessException.of(ErrorCode.SESSION_ALREADY_FINISHED);
	}

	public static BusinessException callAlreadyRevoked() {
		return BusinessException.of(ErrorCode.CALL_ALREADY_REVOKED);
	}
}

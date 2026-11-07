package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import org.springframework.http.HttpStatus;

final class UserErrors {

	private UserErrors() {
	}

	static BusinessException memberLimitExceeded() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "MEMBER_LIMIT_EXCEEDED",
				"자녀 외 멤버는 가족당 최대 5명입니다");
	}

	static BusinessException cannotRemoveGuardian() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "CANNOT_REMOVE_GUARDIAN",
				"보호자는 해제할 수 없습니다");
	}

	static BusinessException memberInActivePickup() {
		return BusinessException.of(HttpStatus.CONFLICT, "MEMBER_IN_ACTIVE_PICKUP",
				"진행 중인 픽업에 묶인 멤버는 해제할 수 없습니다");
	}

	static BusinessException activePickupExists() {
		return BusinessException.of(HttpStatus.CONFLICT, "ACTIVE_PICKUP_EXISTS",
				"진행 중인 픽업이 있어 차량 연결을 해제할 수 없습니다");
	}

	static BusinessException vehicleAlreadyLinked() {
		return BusinessException.of(HttpStatus.CONFLICT, "VEHICLE_ALREADY_LINKED",
				"이미 연결됐거나 연결이 해제된 차량입니다");
	}
}

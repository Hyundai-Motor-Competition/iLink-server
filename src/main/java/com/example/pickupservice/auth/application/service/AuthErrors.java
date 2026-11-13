package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

final class AuthErrors {

	private static final int REISSUE_LIMIT_RETRY_AFTER_SECONDS = 300;

	private AuthErrors() {
	}

	static BusinessException refreshTokenInvalid() {
		return BusinessException.of(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID",
				"refresh 토큰이 만료됐거나 폐기됐습니다");
	}

	static BusinessException vehicleUnlinked() {
		return BusinessException.of(HttpStatus.CONFLICT, "VEHICLE_UNLINKED",
				"연결이 해제된 차량입니다. 앱을 다시 등록해 주세요");
	}

	static BusinessException activePickupExists() {
		return BusinessException.of(ErrorCode.ACTIVE_PICKUP_EXISTS);
	}

	static BusinessException linkCodeReissueLimit() {
		return BusinessException.tooManyRequests("LINK_CODE_REISSUE_LIMIT",
				"연결 코드 재발급 한도를 넘었습니다", REISSUE_LIMIT_RETRY_AFTER_SECONDS);
	}

	static BusinessException linkCodeInvalid() {
		return BusinessException.of(HttpStatus.NOT_FOUND, "LINK_CODE_INVALID",
				"연결 코드가 없거나 이미 사용됐습니다");
	}

	static BusinessException linkCodeExpired() {
		return BusinessException.of(HttpStatus.GONE, "LINK_CODE_EXPIRED",
				"연결 코드가 만료됐거나 이미 사용됐습니다");
	}

	static BusinessException linkCodeAttemptLimit(int retryAfterSeconds) {
		return BusinessException.tooManyRequests("LINK_CODE_ATTEMPT_LIMIT",
				"연결 코드 확인 실패가 많아 잠시 뒤에 다시 시도해 주세요", retryAfterSeconds);
	}

	static BusinessException linkModeMismatch() {
		return BusinessException.of(HttpStatus.CONFLICT, "LINK_MODE_MISMATCH",
				"연결 방식이 차량의 실제 연결 상태와 다릅니다");
	}

	static BusinessException consentRequired() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED",
				"필수 약관에 동의해 주세요");
	}

	static BusinessException deviceAlreadyConnected() {
		return BusinessException.of(HttpStatus.CONFLICT, "DEVICE_ALREADY_CONNECTED",
				"이미 기기가 연결된 멤버입니다");
	}

	static BusinessException inviteAlreadyUsed() {
		return BusinessException.of(HttpStatus.CONFLICT, "INVITE_ALREADY_USED",
				"이미 수락한 초대입니다");
	}

	static BusinessException inviteExpired() {
		return BusinessException.of(HttpStatus.GONE, "INVITE_EXPIRED",
				"만료된 초대입니다");
	}

	static BusinessException notCodeOwner() {
		return BusinessException.of(HttpStatus.FORBIDDEN, "NOT_CODE_OWNER",
				"이 차량이 발급한 연결 코드가 아닙니다");
	}
}

package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import org.springframework.http.HttpStatus;

final class PlaceErrors {

	private PlaceErrors() {
	}

	static BusinessException spotTooFar() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "SPOT_TOO_FAR",
				"장소는 학원에서 500m 안에 있어야 합니다");
	}

	static BusinessException spotInUse() {
		return BusinessException.of(HttpStatus.CONFLICT, "SPOT_IN_USE",
				"진행 중인 픽업이 쓰는 장소는 지울 수 없습니다");
	}

	static BusinessException defaultSpotRequired() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "DEFAULT_SPOT_REQUIRED",
				"다른 장소를 기본 장소로 먼저 지정해야 합니다");
	}

	static BusinessException defaultSpotConflict() {
		return BusinessException.of(HttpStatus.CONFLICT, "DEFAULT_SPOT_CONFLICT",
				"다른 요청이 기본 장소를 먼저 바꿨습니다. 다시 시도해 주세요");
	}
}

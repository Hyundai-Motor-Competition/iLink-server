package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import org.springframework.http.HttpStatus;

final class ScheduleErrors {

	private ScheduleErrors() {
	}

	static BusinessException mapApiUnavailable(Throwable cause) {
		BusinessException exception = BusinessException.of(HttpStatus.BAD_GATEWAY, "MAP_API_UNAVAILABLE",
				"지도 검색을 잠시 사용할 수 없습니다");
		exception.initCause(cause);
		return exception;
	}

	static BusinessException dailyScheduleLimit() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "DAILY_SCHEDULE_LIMIT",
				"같은 요일 일정은 최대 3개입니다");
	}

	static BusinessException settingsVersionConflict() {
		return BusinessException.of(HttpStatus.CONFLICT, "SETTINGS_VERSION_CONFLICT",
				"설정이 변경되었습니다. 다시 확인해 주세요");
	}

	static BusinessException setupIncomplete() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "SETUP_INCOMPLETE",
				"아직 끝나지 않은 설정이 있습니다");
	}
}

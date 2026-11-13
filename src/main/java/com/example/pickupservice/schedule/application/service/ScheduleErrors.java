package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import org.springframework.http.HttpStatus;

import java.util.List;

final class ScheduleErrors {

	private ScheduleErrors() {
	}

	static BusinessException mapApiUnavailable(Throwable cause) {
		BusinessException exception = BusinessException.of(HttpStatus.BAD_GATEWAY, "MAP_API_UNAVAILABLE",
				"지도 검색을 잠시 사용할 수 없습니다");
		exception.initCause(cause);
		return exception;
	}

	static BusinessException scheduleOverlap(List<Long> overlapScheduleIds) {
		return BusinessException.conflictWithOverlap(HttpStatus.CONFLICT, "SCHEDULE_OVERLAP",
				"같은 요일 · 같은 시간에 이미 일정이 있습니다", overlapScheduleIds);
	}

	static BusinessException dailyScheduleLimit() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "DAILY_SCHEDULE_LIMIT",
				"같은 요일 일정은 최대 3개입니다");
	}

	static BusinessException settingsVersionConflict() {
		return BusinessException.of(HttpStatus.CONFLICT, "SETTINGS_VERSION_CONFLICT",
				"설정이 변경되었습니다. 다시 확인해 주세요");
	}

	static BusinessException setupIncomplete(List<MissingSetupItem> missing) {
		return BusinessException.of(ErrorCode.SETUP_INCOMPLETE)
				.withDetail("missing", missing.stream().map(MissingSetupItem::name).toList());
	}
}

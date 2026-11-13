package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public final class PickupErrors {

	private PickupErrors() {
	}

	public static BusinessException notToday() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "NOT_TODAY",
				"오늘 날짜의 픽업만 시작할 수 있습니다");
	}

	public static BusinessException schedulesNotGroupable() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "SCHEDULES_NOT_GROUPABLE",
				"같은 학원 · 같은 시간대의 일정만 함께 시작할 수 있습니다");
	}

	public static BusinessException setupIncomplete(List<String> missing) {
		return BusinessException.of(ErrorCode.SETUP_INCOMPLETE)
				.withDetail("missing", missing);
	}

	public static BusinessException anotherSessionActive() {
		return BusinessException.of(HttpStatus.CONFLICT, "ANOTHER_SESSION_ACTIVE",
				"이미 진행 중인 다른 픽업이 있습니다");
	}

	public static BusinessException spotNotSaved() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "SPOT_NOT_SAVED",
				"저장된 장소가 아닙니다");
	}

	public static BusinessException callAlreadySent() {
		return BusinessException.of(HttpStatus.CONFLICT, "CALL_ALREADY_SENT",
				"이미 합류 호출을 보냈습니다");
	}

	public static BusinessException childAlreadyMoving() {
		return BusinessException.of(HttpStatus.CONFLICT, "CHILD_ALREADY_MOVING",
				"자녀가 이미 이동 중이라 장소를 바꿀 수 없습니다");
	}

	public static BusinessException sessionAlreadyFinished() {
		return BusinessException.of(ErrorCode.SESSION_ALREADY_FINISHED);
	}

	public static BusinessException vehicleNotStopped() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "VEHICLE_NOT_STOPPED",
				"차량이 아직 정차하지 않았습니다");
	}

	public static BusinessException childrenNotAllBoarded() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "CHILDREN_NOT_ALL_BOARDED",
				"아직 탑승하지 않은 자녀가 있습니다");
	}

	public static BusinessException rejectionWindowClosed() {
		return BusinessException.of(HttpStatus.CONFLICT, "REJECTION_WINDOW_CLOSED",
				"탑승을 취소할 수 있는 시간이 지났습니다");
	}

	public static BusinessException incidentAlreadyOpen() {
		return BusinessException.of(HttpStatus.CONFLICT, "INCIDENT_ALREADY_OPEN",
				"이미 처리 중인 예외 상황이 있습니다");
	}

	public static BusinessException prepExtensionLimit() {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "PREP_EXTENSION_LIMIT",
				"준비 시간은 모두 15분까지만 늘릴 수 있습니다");
	}

	public static BusinessException locationConsentRequired() {
		return BusinessException.of(HttpStatus.FORBIDDEN, "LOCATION_CONSENT_REQUIRED",
				"위치 정보 제공 동의가 필요합니다");
	}

	public static BusinessException helpRequestTooSoon(Instant nextAllowedAt, Instant now) {
		return BusinessException.tooManyRequests("HELP_REQUEST_TOO_SOON",
				"잠시 후 다시 요청해 주세요", TimeUtils.secondsUntil(now, nextAllowedAt))
				.withDetail("nextAllowedAt", nextAllowedAt);
	}

	public static BusinessException callGuardUnmet(List<String> unmet) {
		return BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, "CALL_GUARD_UNMET",
				"합류 호출을 보낼 수 있는 조건이 아닙니다")
				.withDetail("unmet", unmet);
	}

	// 여기로 부르기(SES-05 here=true)의 가드 미충족: 409 CALL_GUARD_UNMET(error.unmet)
	public static BusinessException callGuardUnmetConflict(List<String> unmet) {
		return BusinessException.of(HttpStatus.CONFLICT, "CALL_GUARD_UNMET",
				"합류 호출을 보낼 수 있는 조건이 아닙니다")
				.withDetail("unmet", unmet);
	}

	public static BusinessException notGuardianConfirmMode() {
		return BusinessException.of(HttpStatus.CONFLICT, "NOT_GUARDIAN_CONFIRM_MODE",
				"보호자 확인 방식일 때만 직접 호출할 수 있습니다");
	}

	public static BusinessException callAlreadyRevoked() {
		return BusinessException.of(ErrorCode.CALL_ALREADY_REVOKED);
	}

	public static BusinessException childNotCalled() {
		return BusinessException.of(HttpStatus.CONFLICT, "CHILD_NOT_CALLED",
				"합류 안내를 받은 뒤에만 위치를 보낼 수 있습니다");
	}

	public static BusinessException stateVersionConflict(SessionView latest) {
		return BusinessException.stateVersionConflict(latest);
	}

	public static BusinessException notFound() {
		return BusinessException.of(ErrorCode.NOT_FOUND);
	}

	public static BusinessException from(PickupRuleException exception) {
		return switch (exception.violation()) {
			case SESSION_ALREADY_FINISHED -> sessionAlreadyFinished();
			case CHILD_ALREADY_MOVING -> childAlreadyMoving();
			case CALL_ALREADY_REVOKED -> callAlreadyRevoked();
			case PREP_EXTENSION_LIMIT -> prepExtensionLimit();
			case REJECTION_WINDOW_CLOSED -> rejectionWindowClosed();
			case VEHICLE_NOT_STOPPED -> vehicleNotStopped();
			case CHILDREN_NOT_ALL_BOARDED -> childrenNotAllBoarded();
			case INCIDENT_ALREADY_OPEN -> incidentAlreadyOpen();
			case CHILD_NOT_IN_SESSION -> notFound();
		};
	}
}

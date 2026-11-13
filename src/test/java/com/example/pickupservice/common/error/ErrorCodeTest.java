package com.example.pickupservice.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

	@Test
	@DisplayName("common ErrorCode.VEHICLE_ALREADY_LINKED는 409 「이미 보호자가 연결된 차량입니다」다")
	void vehicleAlreadyLinkedIsConflictWithGuardianLinkedMessage() {
		assertThat(ErrorCode.VEHICLE_ALREADY_LINKED.getStatus()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(ErrorCode.VEHICLE_ALREADY_LINKED.getMessage()).isEqualTo("이미 보호자가 연결된 차량입니다");
	}

	@Test
	@DisplayName("BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED)는 409 · code VEHICLE_ALREADY_LINKED · 「이미 보호자가 연결된 차량입니다」다")
	void businessExceptionOfVehicleAlreadyLinkedCarriesCommonCode() {
		BusinessException exception = BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED);

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("VEHICLE_ALREADY_LINKED");
		assertThat(exception.getMessage()).isEqualTo("이미 보호자가 연결된 차량입니다");
	}

	@Test
	@DisplayName("common ErrorCode.RESEND_TOO_SOON은 HTTP 429(TOO_MANY_REQUESTS)이고 문구는 「잠시 뒤에 다시 보내 주세요」다")
	void resendTooSoonIsTooManyRequestsWithResendMessage() {
		assertThat(ErrorCode.RESEND_TOO_SOON.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(ErrorCode.RESEND_TOO_SOON.getMessage()).isEqualTo("잠시 뒤에 다시 보내 주세요");
	}

	@Test
	@DisplayName("common ErrorCode.ACTIVE_PICKUP_EXISTS는 409 「진행 중인 픽업이 있어 처리할 수 없습니다」다(D2)")
	void activePickupExistsIsConflictWithSharedMessage() {
		assertThat(ErrorCode.ACTIVE_PICKUP_EXISTS.getStatus()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(ErrorCode.ACTIVE_PICKUP_EXISTS.getMessage()).isEqualTo("진행 중인 픽업이 있어 처리할 수 없습니다");
	}

	@Test
	@DisplayName("BusinessException.of(ErrorCode.ACTIVE_PICKUP_EXISTS)는 409 · code ACTIVE_PICKUP_EXISTS · 「진행 중인 픽업이 있어 처리할 수 없습니다」다")
	void businessExceptionOfActivePickupExistsCarriesCommonCode() {
		BusinessException exception = BusinessException.of(ErrorCode.ACTIVE_PICKUP_EXISTS);

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("ACTIVE_PICKUP_EXISTS");
		assertThat(exception.getMessage()).isEqualTo("진행 중인 픽업이 있어 처리할 수 없습니다");
	}

	@Test
	@DisplayName("common ErrorCode.SETUP_INCOMPLETE는 422 「픽업 준비가 끝나지 않았습니다」다(D2)")
	void setupIncompleteIsUnprocessableWithSharedMessage() {
		assertThat(ErrorCode.SETUP_INCOMPLETE.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(ErrorCode.SETUP_INCOMPLETE.getMessage()).isEqualTo("픽업 준비가 끝나지 않았습니다");
	}

	@Test
	@DisplayName("BusinessException.of(ErrorCode.SETUP_INCOMPLETE).withDetail(missing, ...)는 422 · code SETUP_INCOMPLETE · 공통 문구이고 missing을 추가 키로 담는다")
	void businessExceptionOfSetupIncompleteCarriesMissingAsDetail() {
		List<String> missing = List.of("PICKUP_SPOT");

		BusinessException exception = BusinessException.of(ErrorCode.SETUP_INCOMPLETE).withDetail("missing", missing);

		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("SETUP_INCOMPLETE");
		assertThat(exception.getMessage()).isEqualTo("픽업 준비가 끝나지 않았습니다");
		assertThat(exception.details()).containsOnlyKeys("missing");
		assertThat(exception.details().get("missing")).isEqualTo(missing);
	}
}

package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.CompleteSetupCommand;
import com.example.pickupservice.schedule.application.port.in.GetSetupSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.SetupChild;
import com.example.pickupservice.schedule.application.port.in.SetupCompletionResult;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CompleteSetupServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long CHILD_ID = 101L;
	private static final int SETTINGS_VERSION = 7;
	private static final int STALE_SETTINGS_VERSION = 6;

	private static final ChildQuery QUERY = new ChildQuery(FAMILY_ID, CHILD_ID);

	@Mock
	private GetSetupSummaryUseCase getSetupSummaryUseCase;

	private CompleteSetupService completeSetupService;

	@BeforeEach
	void setUp() {
		completeSetupService = new CompleteSetupService(getSetupSummaryUseCase);
	}

	@Test
	@DisplayName("설정 완료는 settingsVersion이 다르면 409 SETTINGS_VERSION_CONFLICT로 거절한다")
	void completeSetupRejectsWithSettingsVersionConflictWhenVersionDiffers() {
		given(getSetupSummaryUseCase.getSetupSummary(QUERY)).willReturn(summary(List.of(), SETTINGS_VERSION));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> completeSetupService.completeSetup(
						new CompleteSetupCommand(FAMILY_ID, CHILD_ID, STALE_SETTINGS_VERSION)));

		assertError(thrown, HttpStatus.CONFLICT, "SETTINGS_VERSION_CONFLICT");
	}

	@Test
	@DisplayName("설정 완료는 missing이 남아 있으면 422 SETUP_INCOMPLETE로 거절한다")
	void completeSetupRejectsWithSetupIncompleteWhenMissingRemains() {
		given(getSetupSummaryUseCase.getSetupSummary(QUERY))
				.willReturn(summary(List.of(MissingSetupItem.PICKUP_SPOT), SETTINGS_VERSION));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> completeSetupService.completeSetup(
						new CompleteSetupCommand(FAMILY_ID, CHILD_ID, SETTINGS_VERSION)));

		assertError(thrown, HttpStatus.UNPROCESSABLE_CONTENT, "SETUP_INCOMPLETE");
	}

	@Test
	@DisplayName("설정 완료는 버전도 다르고 missing도 남아 있으면 버전 충돌 409를 먼저 돌려준다")
	void completeSetupChecksVersionBeforeMissing() {
		given(getSetupSummaryUseCase.getSetupSummary(QUERY)).willReturn(summary(
				List.of(MissingSetupItem.CHILD_DEVICE, MissingSetupItem.SCHEDULE), SETTINGS_VERSION));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> completeSetupService.completeSetup(
						new CompleteSetupCommand(FAMILY_ID, CHILD_ID, STALE_SETTINGS_VERSION)));

		assertError(thrown, HttpStatus.CONFLICT, "SETTINGS_VERSION_CONFLICT");
	}

	@Test
	@DisplayName("설정 완료는 버전이 같고 missing이 없으면 syncedToVehicle true를 돌려준다")
	void completeSetupReturnsSyncedToVehicleWhenVersionMatchesAndNothingMissing() {
		given(getSetupSummaryUseCase.getSetupSummary(QUERY)).willReturn(summary(List.of(), SETTINGS_VERSION));

		SetupCompletionResult result = completeSetupService.completeSetup(
				new CompleteSetupCommand(FAMILY_ID, CHILD_ID, SETTINGS_VERSION));

		assertThat(result).isEqualTo(new SetupCompletionResult(true));
	}

	@Test
	@DisplayName("설정 완료는 자녀 확인에 실패하면 NOT_FOUND를 그대로 돌려준다")
	void completeSetupPropagatesNotFoundWhenChildIsNotInFamily() {
		given(getSetupSummaryUseCase.getSetupSummary(QUERY)).willThrow(BusinessException.of(ErrorCode.NOT_FOUND));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> completeSetupService.completeSetup(
						new CompleteSetupCommand(FAMILY_ID, CHILD_ID, SETTINGS_VERSION)));

		assertError(thrown, HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.name());
	}

	private static SetupSummary summary(List<MissingSetupItem> missing, int settingsVersion) {
		return new SetupSummary(
				new SetupChild(CHILD_ID, "민준", DeviceStatus.CONNECTED),
				List.of(),
				List.of(),
				JoinCallMode.AUTO,
				missing,
				settingsVersion);
	}

	private static void assertError(BusinessException thrown, HttpStatus status, String code) {
		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo(code);
		assertThat(thrown.status()).isEqualTo(status);
	}
}

package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.CallModeResult;
import com.example.pickupservice.user.application.port.in.ChangeCallModeCommand;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChangeCallModeServiceTest {

	private static final long FAMILY_ID = 12L;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SaveFamilyPort saveFamilyPort;

	@InjectMocks
	private ChangeCallModeService changeCallModeService;

	@Test
	@DisplayName("합류 안내 방식 변경은 settingsVersion을 1 올린 값을 돌려준다")
	void returnsSettingsVersionIncreasedByOne() {
		given(loadFamilyPort.loadFamily(FAMILY_ID)).willReturn(Optional.of(
				Family.restore(FAMILY_ID, JoinCallMode.AUTO, 7)));
		lenient().when(saveFamilyPort.saveFamily(any())).thenAnswer(invocation -> invocation.getArgument(0));
		ChangeCallModeCommand command = new ChangeCallModeCommand(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM);

		CallModeResult result = changeCallModeService.changeCallMode(command);

		assertThat(result).isEqualTo(new CallModeResult(JoinCallMode.GUARDIAN_CONFIRM, 8));
		ArgumentCaptor<Family> saved = ArgumentCaptor.forClass(Family.class);
		verify(saveFamilyPort, times(1)).saveFamily(saved.capture());
		assertThat(saved.getValue().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getValue().getJoinCallMode()).isEqualTo(JoinCallMode.GUARDIAN_CONFIRM);
		assertThat(saved.getValue().getSettingsVersion()).isEqualTo(8);
	}

	@Test
	@DisplayName("합류 안내 방식 변경은 가족이 없으면 NOT_FOUND로 거절한다")
	void rejectsWhenFamilyMissing() {
		given(loadFamilyPort.loadFamily(FAMILY_ID)).willReturn(Optional.empty());
		ChangeCallModeCommand command = new ChangeCallModeCommand(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeCallModeService.changeCallMode(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveFamilyPort, never()).saveFamily(any());
	}
}

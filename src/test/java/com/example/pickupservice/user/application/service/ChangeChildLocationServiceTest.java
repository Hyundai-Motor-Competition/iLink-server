package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.ChangeChildLocationCommand;
import com.example.pickupservice.user.application.port.in.ChildLocationResult;
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
class ChangeChildLocationServiceTest {

	private static final long FAMILY_ID = 12L;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private SaveFamilyPort saveFamilyPort;

	@InjectMocks
	private ChangeChildLocationService changeChildLocationService;

	@Test
	@DisplayName("자녀 위치 사용 변경은 childLocationEnabled를 바꾸고 settingsVersion을 1 올린 값을 돌려준다")
	void returnsChildLocationEnabledAndSettingsVersionIncreasedByOne() {
		given(loadFamilyPort.loadFamily(FAMILY_ID)).willReturn(Optional.of(
				Family.restore(FAMILY_ID, JoinCallMode.AUTO, 7, false)));
		lenient().when(saveFamilyPort.saveFamily(any())).thenAnswer(invocation -> invocation.getArgument(0));
		ChangeChildLocationCommand command = new ChangeChildLocationCommand(FAMILY_ID, true);

		ChildLocationResult result = changeChildLocationService.changeChildLocation(command);

		assertThat(result).isEqualTo(new ChildLocationResult(true, 8));
		ArgumentCaptor<Family> saved = ArgumentCaptor.forClass(Family.class);
		verify(saveFamilyPort, times(1)).saveFamily(saved.capture());
		assertThat(saved.getValue().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(saved.getValue().isChildLocationEnabled()).isTrue();
		assertThat(saved.getValue().getSettingsVersion()).isEqualTo(8);
		assertThat(saved.getValue().getJoinCallMode()).isEqualTo(JoinCallMode.AUTO);
	}

	@Test
	@DisplayName("자녀 위치 사용을 끄면 childLocationEnabled false와 1 오른 settingsVersion을 돌려주고 저장한다")
	void disablingReturnsFalseWithIncreasedSettingsVersion() {
		given(loadFamilyPort.loadFamily(FAMILY_ID)).willReturn(Optional.of(
				Family.restore(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM, 3, true)));
		lenient().when(saveFamilyPort.saveFamily(any())).thenAnswer(invocation -> invocation.getArgument(0));
		ChangeChildLocationCommand command = new ChangeChildLocationCommand(FAMILY_ID, false);

		ChildLocationResult result = changeChildLocationService.changeChildLocation(command);

		assertThat(result).isEqualTo(new ChildLocationResult(false, 4));
		ArgumentCaptor<Family> saved = ArgumentCaptor.forClass(Family.class);
		verify(saveFamilyPort, times(1)).saveFamily(saved.capture());
		assertThat(saved.getValue().isChildLocationEnabled()).isFalse();
		assertThat(saved.getValue().getSettingsVersion()).isEqualTo(4);
	}

	@Test
	@DisplayName("자녀 위치 사용 변경은 가족이 없으면 NOT_FOUND로 거절한다")
	void rejectsWhenFamilyMissing() {
		given(loadFamilyPort.loadFamily(FAMILY_ID)).willReturn(Optional.empty());
		ChangeChildLocationCommand command = new ChangeChildLocationCommand(FAMILY_ID, true);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> changeChildLocationService.changeChildLocation(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveFamilyPort, never()).saveFamily(any());
	}
}

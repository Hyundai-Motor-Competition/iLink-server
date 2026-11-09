package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.DataSettingsCommand;
import com.example.pickupservice.pickup.application.port.in.DataSettingsResult;
import com.example.pickupservice.pickup.application.port.out.LoadDataSettingPort;
import com.example.pickupservice.pickup.application.port.out.SaveDataSettingPort;
import com.example.pickupservice.pickup.domain.GuardianDataSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateDataSettingsServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final int RETENTION_DAYS = 90;

	@Mock
	private LoadDataSettingPort loadDataSettingPort;

	@Mock
	private SaveDataSettingPort saveDataSettingPort;

	private UpdateDataSettingsService updateDataSettingsService;

	@BeforeEach
	void setUp() {
		updateDataSettingsService = new UpdateDataSettingsService(loadDataSettingPort, saveDataSettingPort,
				PickupProperties.defaults());
		lenient().when(saveDataSettingPort.saveDataSetting(any(GuardianDataSetting.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("데이터 설정 변경은 설정 행이 없는 보호자가 learningConsent true를 보내면 true로 저장하고 learningConsent true · retentionDays 90을 돌려준다")
	void updateDataSettingsWithoutRowSavesConsentAndReturnsRetentionDays() {
		lenient().when(loadDataSettingPort.loadDataSetting(GUARDIAN_ID)).thenReturn(Optional.empty());

		DataSettingsResult result = updateDataSettingsService.updateDataSettings(command(true));

		assertThat(result).isEqualTo(new DataSettingsResult(true, RETENTION_DAYS));
		GuardianDataSetting saved = savedSetting();
		assertThat(saved.getUserId()).isEqualTo(GUARDIAN_ID);
		assertThat(saved.isLearningConsent()).isTrue();
	}

	@Test
	@DisplayName("데이터 설정 변경은 이미 동의한 보호자가 learningConsent false를 보내면 false로 저장하고 learningConsent false · retentionDays 90을 돌려준다")
	void updateDataSettingsWithdrawingConsentSavesFalseAndReturnsRetentionDays() {
		lenient().when(loadDataSettingPort.loadDataSetting(GUARDIAN_ID))
				.thenReturn(Optional.of(GuardianDataSetting.restore(GUARDIAN_ID, true)));

		DataSettingsResult result = updateDataSettingsService.updateDataSettings(command(false));

		assertThat(result).isEqualTo(new DataSettingsResult(false, RETENTION_DAYS));
		GuardianDataSetting saved = savedSetting();
		assertThat(saved.getUserId()).isEqualTo(GUARDIAN_ID);
		assertThat(saved.isLearningConsent()).isFalse();
	}

	private GuardianDataSetting savedSetting() {
		ArgumentCaptor<GuardianDataSetting> captor = ArgumentCaptor.forClass(GuardianDataSetting.class);
		verify(saveDataSettingPort).saveDataSetting(captor.capture());
		return captor.getValue();
	}

	private static DataSettingsCommand command(boolean learningConsent) {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN);
		return new DataSettingsCommand(guardian, learningConsent);
	}

}

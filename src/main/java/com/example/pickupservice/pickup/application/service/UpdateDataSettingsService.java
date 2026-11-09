package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.DataSettingsCommand;
import com.example.pickupservice.pickup.application.port.in.DataSettingsResult;
import com.example.pickupservice.pickup.application.port.in.UpdateDataSettingsUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadDataSettingPort;
import com.example.pickupservice.pickup.application.port.out.SaveDataSettingPort;
import com.example.pickupservice.pickup.domain.GuardianDataSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// OUT-04 보호자 데이터 설정 변경(learningConsent, retentionDays 90)
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateDataSettingsService implements UpdateDataSettingsUseCase {

	private final LoadDataSettingPort loadDataSettingPort;
	private final SaveDataSettingPort saveDataSettingPort;
	private final PickupProperties pickupProperties;

	@Override
	public DataSettingsResult updateDataSettings(DataSettingsCommand command) {
		long userId = command.principal().id();
		GuardianDataSetting setting = loadDataSettingPort.loadDataSetting(userId)
				.orElseGet(() -> GuardianDataSetting.defaultFor(userId));
		setting.changeLearningConsent(command.learningConsent());
		GuardianDataSetting saved = saveDataSettingPort.saveDataSetting(setting);
		return new DataSettingsResult(saved.isLearningConsent(), pickupProperties.dataRetentionDays());
	}

}

package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.application.port.out.LoadDataSettingPort;
import com.example.pickupservice.pickup.application.port.out.SaveDataSettingPort;
import com.example.pickupservice.pickup.domain.GuardianDataSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class GuardianDataSettingPersistenceAdapter implements LoadDataSettingPort, SaveDataSettingPort {

	private final GuardianDataSettingJpaRepository guardianDataSettingJpaRepository;

	@Override
	public Optional<GuardianDataSetting> loadDataSetting(long userId) {
		return guardianDataSettingJpaRepository.findById(userId)
				.map(GuardianDataSettingPersistenceAdapter::toDomain);
	}

	@Override
	public GuardianDataSetting saveDataSetting(GuardianDataSetting setting) {
		return toDomain(guardianDataSettingJpaRepository.save(
				new GuardianDataSettingEntity(setting.getUserId(), setting.isLearningConsent())));
	}

	private static GuardianDataSetting toDomain(GuardianDataSettingEntity entity) {
		return GuardianDataSetting.restore(entity.getUserId(), entity.isLearningConsent());
	}

}

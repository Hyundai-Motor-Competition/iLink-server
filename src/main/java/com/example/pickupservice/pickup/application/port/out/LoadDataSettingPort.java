package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.GuardianDataSetting;

import java.util.Optional;

public interface LoadDataSettingPort {

	Optional<GuardianDataSetting> loadDataSetting(long userId);

}

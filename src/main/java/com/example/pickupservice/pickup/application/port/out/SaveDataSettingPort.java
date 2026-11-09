package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.GuardianDataSetting;

public interface SaveDataSettingPort {

	GuardianDataSetting saveDataSetting(GuardianDataSetting setting);

}

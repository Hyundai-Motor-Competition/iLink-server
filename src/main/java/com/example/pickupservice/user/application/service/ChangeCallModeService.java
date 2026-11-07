package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.CallModeResult;
import com.example.pickupservice.user.application.port.in.ChangeCallModeCommand;
import com.example.pickupservice.user.application.port.in.ChangeCallModeUseCase;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.domain.Family;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeCallModeService implements ChangeCallModeUseCase {

	private final LoadFamilyPort loadFamilyPort;
	private final SaveFamilyPort saveFamilyPort;

	@Override
	@Transactional
	public CallModeResult changeCallMode(ChangeCallModeCommand command) {
		Family family = loadFamilyPort.loadFamily(command.familyId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		family.changeCallMode(command.joinCallMode());
		saveFamilyPort.saveFamily(family);
		return new CallModeResult(family.getJoinCallMode(), family.getSettingsVersion());
	}
}

package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.ChangeChildLocationCommand;
import com.example.pickupservice.user.application.port.in.ChangeChildLocationUseCase;
import com.example.pickupservice.user.application.port.in.ChildLocationResult;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.domain.Family;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeChildLocationService implements ChangeChildLocationUseCase {

	private final LoadFamilyPort loadFamilyPort;
	private final SaveFamilyPort saveFamilyPort;

	@Override
	@Transactional
	public ChildLocationResult changeChildLocation(ChangeChildLocationCommand command) {
		Family family = loadFamilyPort.loadFamily(command.familyId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		family.changeChildLocation(command.enabled());
		saveFamilyPort.saveFamily(family);
		return new ChildLocationResult(family.isChildLocationEnabled(), family.getSettingsVersion());
	}
}

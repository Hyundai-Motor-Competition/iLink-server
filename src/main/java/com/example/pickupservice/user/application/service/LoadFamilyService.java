package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.LoadFamilyUseCase;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.domain.Family;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadFamilyService implements LoadFamilyUseCase {

	private final LoadFamilyPort loadFamilyPort;

	@Override
	public FamilyInfo findFamily(long familyId) {
		Family family = loadFamilyPort.loadFamily(familyId)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		return new FamilyInfo(family.getFamilyId(), family.getJoinCallMode(), family.getSettingsVersion(),
				family.isChildLocationEnabled());
	}
}

package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.in.AcademyInfo;
import com.example.pickupservice.schedule.application.port.in.LoadAcademyUseCase;
import com.example.pickupservice.schedule.application.port.out.LoadAcademyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
public class LoadAcademyService implements LoadAcademyUseCase {

	private final LoadAcademyPort loadAcademyPort;

	@Override
	@Transactional(readOnly = true)
	public Optional<AcademyInfo> findAcademy(long academyId) {
		return loadAcademyPort.loadAcademy(academyId).map(AcademyInfoMapper::toInfo);
	}
}

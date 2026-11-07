package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.in.AcademyInfo;
import com.example.pickupservice.schedule.domain.Academy;

final class AcademyInfoMapper {

	private AcademyInfoMapper() {
	}

	static AcademyInfo toInfo(Academy academy) {
		return new AcademyInfo(academy.getAcademyId(), academy.getName(), academy.getAddress(),
				academy.getLocation(), academy.getEntrance(), academy.getPhone());
	}
}

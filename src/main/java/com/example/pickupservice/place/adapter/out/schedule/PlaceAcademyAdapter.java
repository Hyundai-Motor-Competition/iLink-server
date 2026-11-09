package com.example.pickupservice.place.adapter.out.schedule;

import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.application.port.in.LoadAcademyUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlaceAcademyAdapter implements LoadAcademyPort {

	private final LoadAcademyUseCase loadAcademyUseCase;

	@Override
	public Optional<AcademyInfo> loadAcademy(long academyId) {
		return loadAcademyUseCase.findAcademy(academyId)
				.map(academy -> new AcademyInfo(academy.academyId(), academy.name(), academy.location()));
	}
}

package com.example.pickupservice.schedule.application.port.in;

import java.util.Optional;

public interface LoadAcademyUseCase {

	Optional<AcademyInfo> findAcademy(long academyId);
}

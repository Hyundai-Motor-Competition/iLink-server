package com.example.pickupservice.place.application.port.out;

import java.util.Optional;

public interface LoadAcademyPort {

	Optional<AcademyInfo> loadAcademy(long academyId);
}

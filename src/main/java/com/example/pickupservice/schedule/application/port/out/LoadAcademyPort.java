package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.domain.Academy;

import java.util.Optional;

public interface LoadAcademyPort {

	Optional<Academy> loadAcademy(long academyId);
}

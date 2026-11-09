package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.domain.Academy;

public interface SaveAcademyPort {

	Academy saveAcademy(Academy academy);
}

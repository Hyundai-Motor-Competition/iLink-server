package com.example.pickupservice.pickup.application.port.in;

import java.time.LocalDate;
import java.util.Map;

public interface PickupProgressQueryUseCase {

	Map<Long, PickupProgress> findProgressBySchedule(long familyId, LocalDate date);

}

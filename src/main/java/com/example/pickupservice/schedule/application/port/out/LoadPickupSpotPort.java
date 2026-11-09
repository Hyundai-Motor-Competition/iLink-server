package com.example.pickupservice.schedule.application.port.out;

import java.util.List;

public interface LoadPickupSpotPort {

	List<SpotSummary> loadSpots(long familyId, List<Long> academyIds);
}

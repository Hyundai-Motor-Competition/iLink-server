package com.example.pickupservice.schedule.adapter.out.place;

import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import org.springframework.stereotype.Component;

import java.util.List;

// place 구현 전 임시 동작: 픽업 장소가 없다고 본다
@Component
class SchedulePickupSpotAdapter implements LoadPickupSpotPort {

	@Override
	public List<SpotSummary> loadSpots(long familyId, List<Long> academyIds) {
		return List.of();
	}
}

package com.example.pickupservice.schedule.adapter.out.place;

import com.example.pickupservice.place.application.port.in.LoadPickupSpotUseCase;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class SchedulePickupSpotAdapter implements LoadPickupSpotPort {

	private final LoadPickupSpotUseCase loadPickupSpotUseCase;

	@Override
	public List<SpotSummary> loadSpots(long familyId, List<Long> academyIds) {
		return loadPickupSpotUseCase.findSpots(familyId, academyIds).stream()
				.map(spot -> new SpotSummary(spot.spotId(), spot.academyId(), spot.name(), spot.isDefault()))
				.toList();
	}
}

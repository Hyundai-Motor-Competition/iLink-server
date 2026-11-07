package com.example.pickupservice.place.application.service;

import com.example.pickupservice.place.application.port.in.LoadPickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.place.application.port.in.SpotSummary;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadPickupSpotService implements LoadPickupSpotUseCase {

	private final LoadSpotPort loadSpotPort;

	@Override
	public Optional<SpotInfo> findDefaultSpot(long familyId, long academyId) {
		return loadSpotPort.loadDefaultSpot(familyId, academyId).map(LoadPickupSpotService::toInfo);
	}

	@Override
	public Optional<SpotInfo> findSpot(long spotId) {
		return loadSpotPort.loadSpot(spotId).map(LoadPickupSpotService::toInfo);
	}

	@Override
	public List<SpotSummary> findSpots(long familyId, List<Long> academyIds) {
		if (academyIds.isEmpty()) {
			return List.of();
		}
		Set<Long> academyIdSet = new HashSet<>(academyIds);
		return loadSpotPort.loadSpots(familyId, null).stream()
				.filter(spot -> academyIdSet.contains(spot.getAcademyId()))
				.map(spot -> new SpotSummary(spot.getSpotId(), spot.getAcademyId(), spot.getName(), spot.isDefault()))
				.toList();
	}

	private static SpotInfo toInfo(PickupSpot spot) {
		return new SpotInfo(spot.getSpotId(), spot.getFamilyId(), spot.getAcademyId(), spot.getName(),
				spot.getStopPoint(), spot.getMeetingPoint());
	}
}
